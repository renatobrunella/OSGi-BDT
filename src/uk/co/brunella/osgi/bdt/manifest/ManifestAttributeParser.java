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
package uk.co.brunella.osgi.bdt.manifest;

import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ManifestAttributeParser {

  private Attributes attributes;

  public ManifestAttributeParser(Manifest manifest) {
    attributes = manifest.getMainAttributes();
  }
  
  public Map<String, AttributeElement[]> parseAttributes(String[] attributeNames) {
    Map<String, AttributeElement[]> result = new HashMap<String, AttributeElement[]>(attributeNames.length);
    for (String attributeName : attributeNames) {
      AttributeElement[] elements = parseAttribute(attributes.getValue(attributeName));
      if (elements != null) {
        result.put(attributeName, elements);
      }
    }
    return result;
  }

  private AttributeElement[] parseAttribute(String value) {
    if (value == null) {
      return null;
    }
    List<AttributeElement> elements = new ArrayList<AttributeElement>();
    AttributeElement element = null;
    Tokenizer tokenizer = new Tokenizer(value);
    int type = tokenizer.next();
    while (type != Tokenizer.EOF && type != Tokenizer.ERROR) {
      if (type == Tokenizer.VALUE) {
        if (element == null) {
          element = new AttributeElement();
          elements.add(element);
        }
        element.addValue(tokenizer.getValue());
      } else if (type == Tokenizer.ATTRIBUTE) {
        element.addAttribute(tokenizer.getName(), tokenizer.getValue());
      } else if (type == Tokenizer.DIRECTIVE) {
        element.addDirective(tokenizer.getName(), tokenizer.getValue());
      } else if (type == Tokenizer.SEPARATOR) {
        element = null;
      }
      type = tokenizer.next();
    }
    
    return elements.toArray(new AttributeElement[elements.size()]);
  }
}
