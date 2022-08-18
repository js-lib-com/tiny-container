package com.jslib.container.servlet;

import java.io.FileInputStream;
import java.io.IOException;

import com.jslib.container.mvc.AbstractView;
import com.jslib.util.Files;

public class PageView extends AbstractView {
	@Override
	public void serialize(java.io.OutputStream stream) throws IOException {
		Files.copy(new FileInputStream("src/test/resources/page.html"), stream);
	}
}
