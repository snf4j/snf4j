/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
package org.snf4j.core.thread;

/**
 * A thread supporting fast thread-local variables.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 * 
 */
public class FastThreadLocalThread extends Thread implements IFastThreadLocalThread {

	private Object[] values = new Object[8];
	
	/**
	 * Creates a new thread supporting fast thread-local variables.
	 * 
	 * @see Thread#Thread()
	 */
	public FastThreadLocalThread() {
	}
	
	/**
	 * Creates a new thread supporting fast thread-local variables.
	 * 
	 * @param target the object whose run method is invoked when this thread is
	 *               started. If null, this classes run method does nothing.
	 * @see Thread#Thread(Runnable)
	 */
	public FastThreadLocalThread(Runnable target) {
		super(target);
	}
	
	/**
	 * Creates a new thread supporting fast thread-local variables.
	 * 
	 * @param group  the thread group. If null and there is a security manager, the
	 *               group is determined by SecurityManager.getThreadGroup(). If
	 *               there is not a security manager or
	 *               SecurityManager.getThreadGroup() returns null, the group is set
	 *               to the current thread's thread group.
	 * @param target the object whose run method is invoked when this thread is
	 *               started. If null, this classes run method does nothing.
	 * @throws SecurityException if the current thread cannot create a thread in the
	 *                           specified thread group
	 * @see Thread#Thread(ThreadGroup, Runnable)
	 */
	public FastThreadLocalThread(ThreadGroup group, Runnable target) {
		super(group, target);
	}
	
	/**
	 * Creates a new thread supporting fast thread-local variables.
	 * 
	 * @param name  the name of the new thread.
	 * @see Thread#Thread(String)
	 */
	public FastThreadLocalThread(String name) {
		super(name);
	}
	
	/**
	 * Creates a new thread supporting fast thread-local variables.
	 * 
	 * @param group the thread group. If null and there is a security manager, the
	 *              group is determined by SecurityManager.getThreadGroup(). If
	 *              there is not a security manager or
	 *              SecurityManager.getThreadGroup() returns null, the group is set
	 *              to the current thread's thread group.
	 * @param name the name of the new thread.
	 * @throws SecurityException if the current thread cannot create a thread in the
	 *                           specified thread group
	 * @see Thread#Thread(ThreadGroup, String)
	 */
	public FastThreadLocalThread(ThreadGroup group, String name) {
		super(group, name);
	}
	
	/**
	 * Creates a new thread supporting fast thread-local variables.
	 * 
	 * @param target the object whose run method is invoked when this thread is
	 *               started. If null, this thread's run method is invoked.
	 * @param name   the name of the new thread.
	 * @see Thread#Thread(Runnable, String)
	 */
	public FastThreadLocalThread(Runnable target, String name) {
		super(target, name);
	}
	
	/**
	 * Creates a new thread supporting fast thread-local variables.
	 * 
	 * @param group  the thread group. If null and there is a security manager, the
	 *               group is determined by SecurityManager.getThreadGroup(). If
	 *               there is not a security manager or
	 *               SecurityManager.getThreadGroup() returns null, the group is set
	 *               to the current thread's thread group.
	 * @param target the object whose run method is invoked when this thread is
	 *               started. If null, this thread's run method is invoked.
	 * @param name   the name of the new thread.
	 * @throws SecurityException if the current thread cannot create a thread in the
	 *                           specified thread group
	 * @see Thread#Thread(ThreadGroup, Runnable, String)
	 */
	public FastThreadLocalThread(ThreadGroup group, Runnable target, String name) {
		super(group, target, name);
	}
	
	/**
	 * Creates a new thread supporting fast thread-local variables.
	 * 
	 * @param group     the thread group. If null and there is a security manager,
	 *                  the group is determined by SecurityManager.getThreadGroup().
	 *                  If there is not a security manager or
	 *                  SecurityManager.getThreadGroup() returns null, the group is
	 *                  set to the current thread's thread group.
	 * @param target    the object whose run method is invoked when this thread is
	 *                  started. If null, this thread's run method is invoked.
	 * @param name      the name of the new thread.
	 * @param stackSize the desired stack size for the new thread, or zero to
	 *                  indicate that this parameter is to be ignored.
	 * @throws SecurityException if the current thread cannot create a thread in the
	 *                           specified thread group
	 * @see Thread#Thread(ThreadGroup, Runnable, String, long)
	 */
	public FastThreadLocalThread(ThreadGroup group, Runnable target, String name, long stackSize) {
		super(group, target, name, stackSize);
	}
	
	@Override
	public Object getFastThreadLocal(int index) {
		if (index < values.length) {
			return values[index];
		}
		return null;
	}

	@Override
	public void setFastThreadLocal(int index, Object value) {
		if (index < values.length) {
			values[index] = value;
		}
		else {
			Object[] newValues = new Object[index+1];
			
			System.arraycopy(values, 0, newValues, 0, values.length);
			values = newValues;
			values[index] = value;
		}
	}

	@Override
	public void removeFastThreadLocal(int index) {
		if (index < values.length) {
			values[index] = null;
		}
	}

}
