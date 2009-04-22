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

public class VersionRange implements Serializable {

  private static final long serialVersionUID = -2653177812203862782L;
  
  private static final Version MIN_VERSION = new Version(0, 0, 0);
  private static final Version MAX_VERSION = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

  private Version minVersion;
  private boolean includeMin;
  private Version maxVersion;
  private boolean includeMax;

  public VersionRange(Version minVersion, boolean includeMin, Version maxVersion, boolean includeMax) {
    if (minVersion == null) {
      this.minVersion = MIN_VERSION;
    } else {
      this.minVersion = minVersion;
    }
    this.includeMin = includeMin;
    if (maxVersion == null) {
      this.maxVersion = MAX_VERSION;
    } else {
      this.maxVersion = maxVersion;
    }
    this.includeMax = includeMax;
  }

  public static VersionRange parseVersionRange(String versionRange) {
    versionRange = versionRange.trim();
    if (versionRange == null || versionRange.length() == 0) {
      return new VersionRange(MIN_VERSION, true, MAX_VERSION, true);
    }
    if (versionRange.charAt(0) == '[' || versionRange.charAt(0) == '(') {
      boolean includeMin = versionRange.charAt(0) == '[';
      char last = versionRange.charAt(versionRange.length() - 1);
      int comma = versionRange.indexOf(',');
      if (comma > 0 && (last == ']' || last == ')')) {
        boolean includeMax = last == ']';
        Version minVersion = Version.parseVersion(versionRange.substring(1, comma).trim());
        Version maxVersion = Version.parseVersion(versionRange.substring(comma + 1, versionRange.length() - 1).trim());
        return new VersionRange(minVersion, includeMin, maxVersion, includeMax);
      } else {
        throw new IllegalArgumentException("Invalid version range " + versionRange);
      }
    } else {
      Version minVersion = Version.parseVersion(versionRange);
      return new VersionRange(minVersion, true, MAX_VERSION, true);
    }
  }

  public boolean isIncluded(Version version) {
    int minCompare = minVersion.compareTo(version);
    if (minCompare > 0 || (minCompare == 0 && !includeMin)) {
      return false;
    }
    int maxCompare = maxVersion.compareTo(version);
    if (maxCompare < 0 || (maxCompare == 0 && !includeMax)) {
      return false;
    }
    return true;
  }

  public Version getMinVersion() {
    return minVersion;
  }

  public boolean isIncludeMin() {
    return includeMin;
  }

  public Version getMaxVersion() {
    return maxVersion;
  }

  public boolean isIncludeMax() {
    return includeMax;
  }

  public boolean equals(Object object) {
    if (!(object instanceof VersionRange)) {
      return false;
    }
    VersionRange other = (VersionRange) object;
    return (includeMin == other.includeMin && includeMax == other.includeMax && 
        minVersion.equals(other.minVersion) && maxVersion.equals(other.maxVersion));
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(includeMin ? '[' : '(');
    sb.append(minVersion);
    sb.append(',');
    sb.append(maxVersion);
    sb.append(includeMax ? ']' : ')');
    return sb.toString();
  }
}
