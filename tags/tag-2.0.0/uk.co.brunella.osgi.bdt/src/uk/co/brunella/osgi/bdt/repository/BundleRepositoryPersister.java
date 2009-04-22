/*
 * Copyright 2008 brunella ltd
 *
 * Licensed under the GPL Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package uk.co.brunella.osgi.bdt.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import uk.co.brunella.osgi.bdt.bundle.BundleRepository;

public class BundleRepositoryPersister {

  private static final String REPOSITORY_FILE_NAME = "repository.db";
  private static final String LOCK_FILE_NAME = "repository.lck";

  private File repositoryFile;
  private File lockFile;
  private File repositoryDirectory;

  public BundleRepositoryPersister(File repositoryDirectory) {
    this.repositoryDirectory = repositoryDirectory;
    repositoryFile = new File(repositoryDirectory, REPOSITORY_FILE_NAME);
    lockFile = new File(repositoryDirectory, LOCK_FILE_NAME);
  }
  
  public void save(BundleRepository bundleRepository) throws IOException {
    FileOutputStream fos = null;
    ObjectOutputStream oos = null;
    try {
      fos = new FileOutputStream(repositoryFile);
      oos = new ObjectOutputStream(fos);
      oos.writeObject(bundleRepository);
    } finally {
      if (oos != null) oos.close();
      if (fos != null) fos.close();
    }
  }
  
  public BundleRepository load() throws IOException {
    FileInputStream fis = null;
    ObjectInputStream ois = null;
    BundleRepository repository = null;
    try {
      fis = new FileInputStream(repositoryFile);
      ois = new ObjectInputStream(fis);
      try {
        repository = (BundleRepository) ois.readObject();
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } finally {
      if (ois != null) {
        ois.close();
      }
      if (fis != null) {
        fis.close();
      }
    }
    repository.setLocation(repositoryDirectory);
    return repository;
  }
  
  //TODO is this the right way to lock? What about FileLock?
  public boolean lock(long timeOut) {
    boolean success;
    try {
      success = lockFile.createNewFile();
      while (!success && timeOut > 0) {
        long sleep = timeOut < 100 ? timeOut : 100;
        try {
          timeOut -= sleep;
          Thread.sleep(sleep);
        } catch (InterruptedException e) {
        }
        success = lockFile.createNewFile();
      }
    } catch (IOException e) {
      success = false;
    }
    return success;
  }
  
  public boolean unlock() {
    return lockFile.delete();
  }

  public boolean isLocked() {
    return lockFile.exists();
  }
  
  public boolean checkRepository() {
    return repositoryFile.exists();
  }
}
