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
package uk.co.brunella.osgi.bdt.junit.runner.statement;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FailOnTimeoutStatement extends Statement {
  private Statement fNext;
  private final long fTimeout;

  public FailOnTimeoutStatement(Statement next, long timeout) {
    fNext = next;
    fTimeout = timeout;
  }

  @Override
  public void evaluate() throws Throwable {
    ExecutorService service = Executors.newSingleThreadExecutor();
    Callable<Object> callable = new Callable<Object>() {
      public Object call() throws Exception {
        try {
          fNext.evaluate();
        } catch (Throwable e) {
          throw new ExecutionException(e);
        }
        return null;
      }
    };
    Future<Object> result = service.submit(callable);
    service.shutdown();
    try {
      boolean terminated = service.awaitTermination(fTimeout,
          TimeUnit.MILLISECONDS);
      if (!terminated)
        service.shutdownNow();
      result.get(0, TimeUnit.MILLISECONDS); // throws the exception if one
                                            // occurred during the invocation
    } catch (TimeoutException e) {
      throw new Exception(String.format("test timed out after %d milliseconds",
          fTimeout));
    } catch (ExecutionException e) {
      throw unwrap(e);
    }
  }

  private Throwable unwrap(Throwable e) {
    if (e instanceof ExecutionException)
      return unwrap(e.getCause());
    return e;
  }
}