package com.github.searls.jasmine;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.searls.jasmine.io.RelativizesFilePaths;
import com.github.searls.jasmine.server.JasmineResourceHandler;

@RunWith(MockitoJUnitRunner.class)
public class ServerMojoTest {

	private static final int PORT = 8923;
	private static final String RELATIVE_TARGET_DIR = "some dir";
	private static final String MANUAL_SPEC_RUNNER_NAME = "nacho specs";
	private static final String BASE_DIR = "my-base-dir";
	
	@InjectMocks private ServerMojo subject = new ServerMojo();
	
	@Mock private Log log;
	@Mock private MavenProject mavenProject;
	@Mock private Server server = new Server();
	@Mock private RelativizesFilePaths relativizesFilePaths;
	@Mock private File baseDir;
	@Mock private File targetDir;
	
	@Captor private ArgumentCaptor<SelectChannelConnector> connectorCaptor;
	@Captor private ArgumentCaptor<HandlerList> handlerListCaptor;
	
	@Before
	public void arrangeAndAct() throws Exception {
		subject.setLog(log);
		subject.serverPort = PORT;
		subject.jasmineTargetDir = targetDir;
		subject.manualSpecRunnerHtmlFileName = MANUAL_SPEC_RUNNER_NAME;
		when(baseDir.getAbsolutePath()).thenReturn(BASE_DIR);
		when(mavenProject.getBasedir()).thenReturn(baseDir);
		when(relativizesFilePaths.relativize(baseDir,targetDir)).thenReturn(RELATIVE_TARGET_DIR);
		
		subject.run();
	}
	
	@Test
	public void logsInstructions() {
		verify(log).info(
				"\n\n" +
				"Server started--it's time to spec some JavaScript! You can run your specs as you develop by visiting this URL in a web browser: \n\n\t" +
				"http://localhost:"+PORT+
				"\n\n" +
				"Just leave this process running as you test-drive your code, refreshing your browser window to re-run your specs. You can kill the server with Ctrl-C when you're done.");
	}
	
	@Test
	public void addsConnector() throws Exception {
		verify(server).addConnector(connectorCaptor.capture());
		assertThat(connectorCaptor.getValue(),is(SelectChannelConnector.class));
		assertThat(connectorCaptor.getValue().getPort(),is(PORT));
	}
	
	@Test
	public void addsResourceHandler() throws Exception {
		verify(server).setHandler(handlerListCaptor.capture());
		ResourceHandler handler = (ResourceHandler) handlerListCaptor.getValue().getHandlers()[0];
		
		assertThat(handler,is(JasmineResourceHandler.class));
		assertThat(handler.isDirectoriesListed(),is(true));
		assertThat(handler.getWelcomeFiles()[0],is(RELATIVE_TARGET_DIR+File.separator+MANUAL_SPEC_RUNNER_NAME));
		assertThat(handler.getResourceBase(),is(Resource.newResource(BASE_DIR).toString()));
	}
	
	@Test
	public void addsDefaultHandler() throws Exception {
		verify(server).setHandler(handlerListCaptor.capture());
		assertThat(handlerListCaptor.getValue().getHandlers()[1],is(DefaultHandler.class));
	}
	
	@Test
	public void startsTheServer() throws Exception {
		verify(server).start();
	}
	
	@Test
	public void joinsTheServer() throws Exception {
		verify(server).join();
	}
	
	
	
	
}