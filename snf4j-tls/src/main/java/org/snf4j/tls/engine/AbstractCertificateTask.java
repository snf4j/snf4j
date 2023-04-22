/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * -----------------------------------------------------------------------------
 */
package org.snf4j.tls.engine;

abstract public class AbstractCertificateTask extends AbstractEngineTask {

	private final ICertificateSelector selector;
	
	private final CertificateCriteria criteria;
	
	protected volatile SelectedCertificates certificates;
	
	AbstractCertificateTask(ICertificateSelector selector, CertificateCriteria criteria) {
		this.selector = selector;
		this.criteria = criteria;
	}

	AbstractCertificateTask() {
		this.selector = null;
		this.criteria = null;
	}
	
	@Override
	public boolean isProducing() {
		return true;
	}

	@Override
	public String name() {
		return "Certificate";
	}

	@Override
	void execute() throws Exception {
		if (selector != null) {
			certificates = selector.selectCertificates(criteria);
		}
	}

}
