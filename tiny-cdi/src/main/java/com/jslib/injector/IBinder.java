package com.jslib.injector;

public interface IBinder {

	<T> IBindingBuilder<T> bind(Class<T> type);
	
}
