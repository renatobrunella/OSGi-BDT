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
package uk.co.brunella.osgi.bdt.bundle;

import java.io.Serializable;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class Version implements Comparable<Version>, Serializable {

  private static final long serialVersionUID = 6381635399120861082L;

  private final int major;
  private final int minor;
  private final int micro;
  private final String qualifier;
  private static final String SEPARATOR = ".";

  public static final Version emptyVersion = new Version(0, 0, 0);

  public Version(int major, int minor, int micro) {
    this(major, minor, micro, null);
  }

  public Version(int major, int minor, int micro, String qualifier) {
    if (qualifier == null) {
      qualifier = "";
    }

    this.major = major;
    this.minor = minor;
    this.micro = micro;
    this.qualifier = qualifier;
    validate();
  }

  public Version(String version) {
    int major = 0;
    int minor = 0;
    int micro = 0;
    String qualifier = "";

    try {
      StringTokenizer st = new StringTokenizer(version, SEPARATOR, true);
      major = Integer.parseInt(st.nextToken());

      if (st.hasMoreTokens()) {
        st.nextToken(); // consume delimiter
        minor = Integer.parseInt(st.nextToken());

        if (st.hasMoreTokens()) {
          st.nextToken(); // consume delimiter
          micro = Integer.parseInt(st.nextToken());

          if (st.hasMoreTokens()) {
            st.nextToken(); // consume delimiter
            qualifier = st.nextToken();

            if (st.hasMoreTokens()) {
              throw new IllegalArgumentException("invalid format");
            }
          }
        }
      }
    } catch (NoSuchElementException e) {
      throw new IllegalArgumentException("invalid format");
    }

    this.major = major;
    this.minor = minor;
    this.micro = micro;
    this.qualifier = qualifier;
    validate();
  }

  private void validate() {
    if (major < 0) {
      throw new IllegalArgumentException("negative major");
    }
    if (minor < 0) {
      throw new IllegalArgumentException("negative minor");
    }
    if (micro < 0) {
      throw new IllegalArgumentException("negative micro");
    }
    int length = qualifier.length();
    for (int i = 0; i < length; i++) {
      if ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-".indexOf(qualifier.charAt(i)) == -1) {
        throw new IllegalArgumentException("invalid qualifier");
      }
    }
  }

  public static Version parseVersion(String version) {
    if (version == null) {
      return emptyVersion;
    }

    version = version.trim();
    if (version.length() == 0) {
      return emptyVersion;
    }

    return new Version(version);
  }

  public int getMajor() {
    return major;
  }

  public int getMinor() {
    return minor;
  }

  public int getMicro() {
    return micro;
  }

  public String getQualifier() {
    return qualifier;
  }

  public String toString() {
    String base = major + SEPARATOR + minor + SEPARATOR + micro;
    if (qualifier.length() == 0) {
      return base;
    } else {
      return base + SEPARATOR + qualifier;
    }
  }

  public int hashCode() {
    return (major << 24) + (minor << 16) + (micro << 8) + qualifier.hashCode();
  }

  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }

    if (!(object instanceof Version)) {
      return false;
    }

    Version other = (Version) object;
    return (major == other.major) && (minor == other.minor) && (micro == other.micro)
        && qualifier.equals(other.qualifier);
  }

  public int compareTo(Version other) {
    if (other == this) {
      return 0;
    }

    int result = major - other.major;
    if (result != 0) {
      return result;
    }

    result = minor - other.minor;
    if (result != 0) {
      return result;
    }

    result = micro - other.micro;
    if (result != 0) {
      return result;
    }

    return qualifier.compareTo(other.qualifier);
  }
}
