package com.ibbignerd.MediaControlsSSH;

import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SSHManager {
	private static final Logger LOGGER = Logger.getLogger(SSHManager.class.getName());
	private final int intConnectionPort;
	private final int intTimeOut;
	private JSch jschSSHChannel;
	private String strUserName;
	private String strConnectionIP;
	private String strPassword;
	private Session sesConnection;

	public SSHManager(String userName, String password, String connectionIP, String knownHostsFileName) {
		doCommonConstructorActions(userName, password, connectionIP, knownHostsFileName);

		intConnectionPort = 22;
		intTimeOut = 60000;
	}

	public SSHManager(String userName, String password, String connectionIP, String knownHostsFileName,
			int connectionPort) {
		doCommonConstructorActions(userName, password, connectionIP, knownHostsFileName);

		intConnectionPort = connectionPort;
		intTimeOut = 10000;
	}

	public SSHManager(String userName, String password, String connectionIP, String knownHostsFileName,
			int connectionPort, int timeOutMilliseconds) {
		doCommonConstructorActions(userName, password, connectionIP, knownHostsFileName);

		intConnectionPort = connectionPort;
		intTimeOut = timeOutMilliseconds;
	}

	private void doCommonConstructorActions(String userName, String password, String connectionIP,
			String knownHostsFileName) {
		jschSSHChannel = new JSch();
		try {
			jschSSHChannel.setKnownHosts(knownHostsFileName);
		} catch (JSchException jschX) {
			logError(jschX.getMessage());
		}
		strUserName = userName;
		strPassword = password;
		strConnectionIP = connectionIP;
	}

	public String connect() {
		String errorMessage = "";
		try {
			sesConnection = jschSSHChannel.getSession(strUserName, strConnectionIP, intConnectionPort);

			sesConnection.setPassword(strPassword);
			sesConnection.setConfig("StrictHostKeyChecking", "no");
			sesConnection.connect(intTimeOut);
			MediaControlsSSH_UI.debugLogString("Successful connection!");
		} catch (JSchException jschX) {
			errorMessage = jschX.getMessage();
			MediaControlsSSH_UI.debugLogString("Failed to connect with error: " + jschX.getMessage());
		}
		return errorMessage;
	}

	private String logError(String errorMessage) {
		if (errorMessage != null) {
			LOGGER.log(Level.SEVERE, "{0}:{1} - {2}",
				new Object[] { strConnectionIP, Integer.valueOf(intConnectionPort), errorMessage });
		}
		return errorMessage;
	}

	private String logWarning(String warnMessage) {
		if (warnMessage != null) {
			LOGGER.log(Level.WARNING, "{0}:{1} - {2}",
				new Object[] { strConnectionIP, Integer.valueOf(intConnectionPort), warnMessage });
		}
		return warnMessage;
	}

	public boolean isAlive() {
		return sesConnection.isConnected();
	}

	public String sendCommand(String command) {
		if ((!command.contains("info")) && (!command.contains("isRadio")) && (!command.contains("app"))
				&& (!command.contains("length")) && (!command.contains("elapsed"))) {
			MediaControlsSSH_UI.debugLogString("Sending command: " + command);
		}
		StringBuilder outputBuffer = new StringBuilder();
		try {
			Channel channel = sesConnection.openChannel("exec");
			((ChannelExec)channel).setCommand(command);
			channel.setOutputStream(System.out);
			channel.connect();
			InputStream commandOutput = channel.getInputStream();
			int readByte = commandOutput.read();
			while (readByte != -1) {
				outputBuffer.append((char)readByte);
				readByte = commandOutput.read();
			}
			channel.disconnect();
		} catch (JSchException ioX) {
			ioX.printStackTrace();
			MediaControlsSSH_UI.debugLogString("Failed sending command: " + command + ".\n With error: "
					+ ioX.getMessage());
			return null;
		} catch (IOException ioX) {
			ioX.printStackTrace();
			MediaControlsSSH_UI.debugLogString("Failed sending command: " + command + ".\n With error: "
					+ ioX.getMessage());
			return null;
		}
		return outputBuffer.toString();
	}

	public void close() {
		sesConnection.disconnect();
		MediaControlsSSH_UI.debugLogString("Disconnecting from host.");
	}
}
