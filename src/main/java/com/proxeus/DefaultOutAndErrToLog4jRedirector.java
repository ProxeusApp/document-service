package com.proxeus;

import org.apache.log4j.Logger;

import java.io.PrintStream;

public class DefaultOutAndErrToLog4jRedirector {
		private final Logger logger = Logger.getLogger("Default");

    public DefaultOutAndErrToLog4jRedirector(){
			tieSystemOutAndErrToLog();
		}
		public void tieSystemOutAndErrToLog() {
			System.setOut(createLoggingProxy(System.out));
			System.setErr(createLoggingProxy(System.err));
		}

		public PrintStream createLoggingProxy(final PrintStream realPrintStream) {
			return new PrintStream(realPrintStream) {
				public void print(final String string) {
					logger.info(string);
				}
			};
		}
	}