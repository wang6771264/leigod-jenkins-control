package org.codinjutsu.tools.jenkins.util;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;

public class HtmlClipboard {

	public static void main(String[] args) {
		String link = "http://172.31.4.7:8090/jenkins/job/bohe-netbar-market-build/279/";
		String displayName = "#279";
		String htmlText = String.format("<html><a href=\"%s\">%s</a></html>", link, displayName);

		copyHtmlToClipboard(displayName, htmlText);
	}

	public static void copyHtmlToClipboard(String plainText, String url) {
		String htmlText = String.format("<html><a href=\"%s\">%s</a></html>", url, plainText);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

		// 自定义一个HTML格式的DataFlavor
		DataFlavor htmlFlavor = new DataFlavor("text/html;class=java.lang.String", "HTML Format");

		// 创建一个Transferable对象
		Transferable transferable = new Transferable() {
			public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[]{DataFlavor.stringFlavor, htmlFlavor};
			}

			public boolean isDataFlavorSupported(DataFlavor flavor) {
				return DataFlavor.stringFlavor.equals(flavor) || htmlFlavor.equals(flavor);
			}

			@NotNull
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
				if (DataFlavor.stringFlavor.equals(flavor)) {
					return plainText;
				} else if (htmlFlavor.equals(flavor)) {
					return htmlText;
				}
				throw new UnsupportedFlavorException(flavor);
			}
		};

		clipboard.setContents(transferable, null);
	}
}
