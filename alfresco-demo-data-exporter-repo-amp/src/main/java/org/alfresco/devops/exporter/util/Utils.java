package org.alfresco.devops.exporter.util;

public final class Utils {
	
	private Utils(){
        throw new AssertionError();
	};
	
	public static boolean isNullOrEmpty(String paramSelf) {
		return paramSelf == null || paramSelf.trim().isEmpty();
	}

}
