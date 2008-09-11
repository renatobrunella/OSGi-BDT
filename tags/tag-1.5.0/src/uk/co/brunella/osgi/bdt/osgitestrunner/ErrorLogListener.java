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
package uk.co.brunella.osgi.bdt.osgitestrunner;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

public class ErrorLogListener implements LogListener, ServiceListener {

  private BundleContext context;
  private ServiceReference readerServiceReference;
  private LogReaderService readerService;

  public ErrorLogListener(BundleContext context) throws InvalidSyntaxException {
    this.context = context;
    synchronized (this) {
      String filter = "(objectclass=" + LogReaderService.class.getName() + ")";
      context.addServiceListener(this, filter);
      ServiceReference[] refs;
      refs = context.getServiceReferences(LogReaderService.class.getName(), null);
      for (int i = 0; refs != null && i < refs.length; i++) {
        serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, refs[i]));
      }
    }

  }

  public void logged(LogEntry entry) {
    System.err.println( "[" + entry.getBundle().getBundleId() + ":"
        + entry.getBundle().getLocation() + "] "
        + "L" + entry.getLevel() + ": "
        + entry.getMessage());
  }

  public void close() {
    if (readerService != null) {
      readerService.removeLogListener(this);
    }
    if (readerServiceReference != null) {
      context.ungetService(readerServiceReference);
    }
  }

  public void serviceChanged(ServiceEvent event) {
    ServiceReference serviceReference = event.getServiceReference();
    switch (event.getType()) {
    case ServiceEvent.REGISTERED:
      if (readerServiceReference == null) {
        readerServiceReference = serviceReference;
        readerService = (LogReaderService) context.getService(serviceReference);
        if (readerService != null) {
          readerService.addLogListener(this);
        }
      }
      break;
    case ServiceEvent.UNREGISTERING:
      if (readerServiceReference == serviceReference) {
        close();
        readerServiceReference = null;
      }
      break;
    }
  }

}
