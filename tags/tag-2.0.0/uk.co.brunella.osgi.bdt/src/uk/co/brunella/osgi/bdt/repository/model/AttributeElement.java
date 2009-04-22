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
package uk.co.brunella.osgi.bdt.repository.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttributeElement {

  private List<String> values = new ArrayList<String>();
  private Map<String, String[]> attributes = new HashMap<String, String[]>();
  private Map<String, String[]> directives = new HashMap<String, String[]>();

  public List<String> getValues() {
    return values;
  }

  public Map<String, String[]> getAttributes() {
    return attributes;
  }

  public String[] getAttributeValues(String name) {
    return attributes.get(name);
  }

  public Map<String, String[]> getDirectives() {
    return directives;
  }

  public String[] getDirectiveValues(String name) {
    return directives.get(name);
  }

  public void addValue(String value) {
    values.add(value);
  }

  public void addAttribute(String name, String value) {
    String[] oldAttributes = attributes.get(name);
    String[] newAttributes;
    if (oldAttributes == null) {
      newAttributes = new String[] { value };
    } else {
      newAttributes = new String[oldAttributes.length + 1];
      System.arraycopy(oldAttributes, 0, newAttributes, 0, oldAttributes.length);
      newAttributes[newAttributes.length - 1] = value;
    }
    attributes.put(name, newAttributes);
  }

  public void addDirective(String name, String value) {
    String[] oldDirectives = directives.get(name);
    String[] newDirectives;
    if (oldDirectives == null) {
      newDirectives = new String[] { value };
    } else {
      newDirectives = new String[oldDirectives.length + 1];
      System.arraycopy(oldDirectives, 0, newDirectives, 0, oldDirectives.length);
      newDirectives[newDirectives.length - 1] = value;
    }
    directives.put(name, newDirectives);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < values.size(); i++) {
      if (i != 0) {
        sb.append(", ");
      }
      sb.append(values.get(i));
    }
    sb.append('\n');
    for (String name : attributes.keySet()) {
      sb.append("\tAttribute: ").append(name).append('\n');
      String[] values = attributes.get(name);
      for (int i = 0; i < values.length; i++) {
        sb.append("\t\t").append(values[i]).append('\n');
      }
    }
    for (String name : directives.keySet()) {
      sb.append("\tDirective: ").append(name).append('\n');
      String[] values = directives.get(name);
      for (int i = 0; i < values.length; i++) {
        sb.append("\t\t").append(values[i]).append('\n');
      }
    }
    return sb.toString();
  }
}
