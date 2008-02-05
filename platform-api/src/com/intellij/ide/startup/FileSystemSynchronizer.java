/*
 * Copyright 2000-2007 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.ide.startup;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author max
 */
public class FileSystemSynchronizer {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.startup.FileSystemSynchronizer");

  private ArrayList<CacheUpdater> myUpdaters = new ArrayList<CacheUpdater>();
  private LinkedHashSet<VirtualFile> myFilesToUpdate = new LinkedHashSet<VirtualFile>();
  private Collection/*<VirtualFile>*/[] myUpdateSets;

  private boolean myIsCancelable = false;

  public void registerCacheUpdater(@NotNull CacheUpdater cacheUpdater) {
    myUpdaters.add(cacheUpdater);
  }

  public void setCancelable(boolean isCancelable) {
    myIsCancelable = isCancelable;
  }

  public void execute() {
    /*
    long time1 = System.currentTimeMillis();
    */

    if (!myIsCancelable) {
      ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
      if (indicator != null) {
        indicator.startNonCancelableSection();
      }
    }

    try {
      if (myUpdateSets == null) { // collectFilesToUpdate() was not executed before
        if (collectFilesToUpdate() == 0) return;
      }

      updateFiles();
    }
    catch (ProcessCanceledException e) {
      for (CacheUpdater updater : myUpdaters) {
        if (updater != null) {
          updater.canceled();
        }
      }
      throw e;
    }
    finally {
      if (!myIsCancelable) {
        ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
        if (indicator != null) {
          indicator.finishNonCancelableSection();
        }
      }
    }

    /*
    long time2 = System.currentTimeMillis();
    System.out.println("synchronizer.execute() in " + (time2 - time1) + " ms");
    */
  }

  public int collectFilesToUpdate() {
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.pushState();
      indicator.setText(IdeBundle.message("progress.scanning.files"));
    }

    myUpdateSets = new Collection[myUpdaters.size()];
    for (int i = 0; i < myUpdaters.size(); i++) {
      CacheUpdater updater = myUpdaters.get(i);
      try {
        VirtualFile[] updaterFiles = updater.queryNeededFiles();
        Collection<VirtualFile> localSet = new LinkedHashSet<VirtualFile>(Arrays.asList(updaterFiles));
        myFilesToUpdate.addAll(localSet);
        myUpdateSets[i] = localSet;
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Throwable e) {
        LOG.error(e);
        myUpdateSets[i] = new ArrayList();
      }
    }

    if (indicator != null) {
      indicator.popState();
    }

    if (myFilesToUpdate.isEmpty()) {
      updatingDone();
    }

    return myFilesToUpdate.size();
  }

  private void updateFiles() {
    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      indicator.pushState();
      indicator.setText(IdeBundle.message("progress.parsing.files"));
    }

    int totalFiles = myFilesToUpdate.size();
    final MyContentQueue contentQueue = new MyContentQueue();

    final Runnable contentLoadingRunnable = new Runnable() {
      public void run() {
        try {
          for (VirtualFile file : myFilesToUpdate) {
            if (indicator != null) indicator.checkCanceled();
            contentQueue.put(file);
          }
        }
        catch (ProcessCanceledException e) {
          // Do nothing, exit the thread.
        }
        catch (InterruptedException e) {
          LOG.error(e);
        }
        finally {
          try {
            contentQueue.put(new FileContent(null));
          }
          catch (InterruptedException e) {
            LOG.error(e);
          }
        }
      }
    };

    ApplicationManager.getApplication().executeOnPooledThread(contentLoadingRunnable);

    int count = 0;
    while (true) {
      FileContent content = null;
      try {
        content = contentQueue.take();
      }
      catch (InterruptedException e) {
        LOG.error(e);
      }
      if (content == null) break;
      final VirtualFile file = content.getVirtualFile();
      if (file == null) break;
      if (indicator != null) {
        indicator.checkCanceled();
        indicator.setFraction((double)++count / totalFiles);
        indicator.setText2(file.getPresentableUrl());
      }
      for (int i = 0; i < myUpdaters.size(); i++) {
        CacheUpdater updater = myUpdaters.get(i);
        if (myUpdateSets[i].remove(file)) {
          try {
            updater.processFile(content);
          }
          catch (ProcessCanceledException e) {
            throw e;
          }
          catch (Throwable e) {
            LOG.error(e);
          }
          if (myUpdateSets[i].isEmpty()) {
            try {
              updater.updatingDone();
            }
            catch (ProcessCanceledException e) {
              throw e;
            }
            catch (Throwable e) {
              LOG.error(e);
            }
            myUpdaters.set(i, null);
          }
        }
      }
    }

    updatingDone();

    if (indicator != null) {
      indicator.popState();
    }
  }

  private void updatingDone() {
    for (CacheUpdater updater : myUpdaters) {
      try {
        if (updater != null) updater.updatingDone();
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }

    dropUpdaters();
  }

  private void dropUpdaters() {
    myUpdaters.clear();
    myFilesToUpdate.clear();
    myUpdateSets = null;
  }

  @SuppressWarnings({"SynchronizeOnThis"})
  private static class MyContentQueue extends ArrayBlockingQueue<FileContent> {
    private long totalSize;
    private static final long SIZE_THRESHOLD = 1024*1024;

    public MyContentQueue() {
      super(256);
      totalSize = 0;
    }

    @SuppressWarnings({"MethodOverloadsMethodOfSuperclass"})
    public void put(VirtualFile file) throws InterruptedException {
      ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();

      FileContent content;
      synchronized (this) {
        content = new FileContent(file);
        if (file.isValid()) {
          try {
            if (content.getLength() < SIZE_THRESHOLD) {
              while (totalSize > SIZE_THRESHOLD) {
                if (indicator != null) indicator.checkCanceled();
                wait(300);
              }

              totalSize += content.getBytes().length;
            }
          }
          catch (IOException e) {
            content.setEmptyContent();
          }
          catch(ProcessCanceledException e) {
            throw e;
          }
          catch (Throwable e) {
            LOG.error(e);
          }
        }
        else {
          content.setEmptyContent();
        }
      }

      put(content);
    }


    public FileContent take() throws InterruptedException {
      final FileContent result = super.take();

      synchronized (this) {
        try {
          final VirtualFile file = result.getVirtualFile();
          if (file == null || !file.isValid() || result.getLength() >= SIZE_THRESHOLD) return result;
          totalSize -= result.getBytes().length;
        }
        catch (IOException e) {
          LOG.error(e);
        }
        finally {
          notifyAll();
        }
      }

      return result;
    }
  }
}
