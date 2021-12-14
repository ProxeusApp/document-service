package com.proxeus;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.PrintStream;

public class DefaultOutAndErrToLog4jRedirector {
		private final Logger logger = LogManager.getLogger("Default");

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
