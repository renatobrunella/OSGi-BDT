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
package uk.co.brunella.osgi.bdt.util;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FileUtils {

  public static boolean deleteDir(File directory) {
    boolean ok = true;
    File[] children = directory.listFiles();
    if (children != null) {
      for (File file : children) {
        if (!file.isDirectory()) {
          ok = ok && file.delete();
        } else {
          ok = ok && deleteDir(file);
        }
      }
    }
    ok = ok && directory.delete();
    return ok;
  }

  public static void copyFile(File source, File dest) throws IOException {
    if (source.equals(dest)) {
      return;
    }
    InputStream in = null;
    OutputStream out = null;
    try {
      in = new FileInputStream(source);
      out = new FileOutputStream(dest);
      byte[] buffer = new byte[1024];
      int count = 0;
      do {
        out.write(buffer, 0, count);
        count = in.read(buffer, 0, buffer.length);
      } while (count != -1);
    } finally {
      if (in != null) {
        in.close();
      }
      if (out != null) {
        out.close();
      }
    }
  }

  public static void extractJar(JarFile jarFile, File baseDirectory, boolean extractEmbeddedJars) throws IOException {
    Enumeration<JarEntry> entries = jarFile.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      if (entry.isDirectory()) {
        File directory = new File(baseDirectory, entry.getName());
        directory.mkdirs();
      } else {
        File destFile = new File(baseDirectory, entry.getName());
        if (!destFile.exists()) {
          destFile.getParentFile().mkdirs();
          InputStream in = null;
          OutputStream out = null;
          try {
            in = jarFile.getInputStream(entry);
            out = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int count = 0;
            do {
              out.write(buffer, 0, count);
              count = in.read(buffer, 0, buffer.length);
            } while (count != -1);
          } finally {
            if (in != null) {
              in.close();
            }
            if (out != null) {
              out.close();
            }
          }
          if (extractEmbeddedJars && destFile.getName().toLowerCase().endsWith(".jar")) {
            JarFile embeddedJar = new JarFile(destFile);
            File embeddedBaseDirectory = destFile.getParentFile();
            extractJar(embeddedJar, embeddedBaseDirectory, false);
            embeddedJar.close();
          }
        }
      }
    }
  }
}
