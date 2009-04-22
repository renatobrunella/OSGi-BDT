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

public class Tokenizer {
  public final static int ERROR = -1;
  public final static int EOF = 0;
  public final static int SEPARATOR = 1;
  public final static int VALUE = 2;
  public final static int ATTRIBUTE = 3;
  public final static int DIRECTIVE = 4;

  protected char avalue[];
  protected int max;
  protected int cursor;
  private String name;
  private String value;

  public Tokenizer(String value) {
    this.avalue = value.toCharArray();
    max = this.avalue.length;
    cursor = 0;
  }

  public String getName() {
    return name;
  }
  
  public String getValue() {
    return value;
  }
  
  public int next() {
    name = null;
    value = null;
    String next = getString(";,:=");
    char c = getChar();
    if (next == null && c == '\0') {
      return EOF;
    } else if (next == null && c == ',') {
      return SEPARATOR;
    } else if (c == ';' || c == ',' || c == '\0') {
      value = next;
      if (c == ',') {
        cursor--;
      }
      return VALUE;
    } else if (c == '=') {
      name = next;
      value = getString(";,:=");
      c = getChar();
      if (c == ',') {
        cursor--;
      }
      return ATTRIBUTE;
    } else if (c == ':') {
      if ((c = getChar()) == '=') {
        name = next;
        value = getString(";,:=");
        c = getChar();
        if (c == ',') {
          cursor--;
        }
        return DIRECTIVE;
      } else {
        return ERROR;
      }
    } else if (c == ',') {
      return SEPARATOR;
    } else {
      return ERROR;
    }
  }

  private void skipWhiteSpace() {
    char[] val = avalue;
    int cur = cursor;

    for (; cur < max; cur++) {
      char c = val[cur];
      if ((c == ' ') || (c == '\t') || (c == '\n') || (c == '\r')) {
        continue;
      }
      break;
    }
    cursor = cur;
  }

  protected String getString(String terminals) {
    skipWhiteSpace();
    char[] val = avalue;
    int cur = cursor;

    if (cur < max) {
      if (val[cur] == '\"') /* if a quoted string */
      {
        StringBuffer sb = new StringBuffer();
        cur++; /* skip quote */
        char c = '\0';
        int begin = cur;
        for (; cur < max; cur++) {
          c = val[cur];
          // this is an escaped char
          if (c == '\\') {
            cur++; // skip the escape char
            if (cur == max)
              break;
            c = val[cur]; // include the escaped char
          } else if (c == '\"') {
            break;
          }
          sb.append(c);
        }
        int count = cur - begin;
        if (c == '\"') {
          cur++;
        }
        cursor = cur;
        if (count > 0) {
          skipWhiteSpace();
          return sb.toString();
        }
      } else /* not a quoted string; same as token */
      {
        int begin = cur;
        for (; cur < max; cur++) {
          char c = val[cur];
          if ((terminals.indexOf(c) != -1)) {
            break;
          }
        }
        cursor = cur;
        int count = cur - begin;
        if (count > 0) {
          skipWhiteSpace();
          while (count > 0 && (val[begin + count - 1] == ' ' || val[begin + count - 1] == '\t'))
            count--;
          return (new String(val, begin, count));
        }
        return (null);
      }
    }
    return (null);
  }

  protected char getChar() {
    int cur = cursor;
    if (cur < max) {
      cursor = cur + 1;
      return (avalue[cur]);
    }
    return ('\0'); /* end of value */
  }

  public boolean hasMoreTokens() {
    if (cursor < max) {
      return true;
    }
    return false;
  }
}
