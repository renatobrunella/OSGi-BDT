/*
 * Copyright 2009 brunella ltd
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
package uk.co.brunella.osgi.bdt.junit.annotation;

/**
 * Annotation used to describe a directory relative to the base directory
 * that should be included in the test bundle. Source and destination need
 * to be specified. If the destination is "" then the content of the source
 * directory will be included at the root of the jar file.
 * 
 * <p>Example:</p>
 * <code>&#64;Include(source = "bin", dest = "")</code>
 * <p/>
 * <code>&#64;Include(source = "META-INF", dest = "META-INF")</code>
 * <p/>
 *
 * @see OSGiBDTTest
 */
public @interface Include {
  
  /**
   * Source directory relative to the base directory. 
   */
  String source();
  
  /**
   * Destination path in the jar file. <code>""</code> is the root. 
   */
  String dest();
}
