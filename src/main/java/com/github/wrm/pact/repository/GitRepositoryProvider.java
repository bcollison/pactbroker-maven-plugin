package com.github.wrm.pact.repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

import com.github.wrm.pact.domain.PactFile;
import com.github.wrm.pact.git.GitApi;

/**
 * interface for a git-based repository.
 * clones a remote repository, adds all files and pushes changes
 * @author pmucha
 *
 */
public class GitRepositoryProvider implements RepositoryProvider{

	
	String path = "target/pact-git-temp";
	
	Log log;

	private String url;
	
	public GitRepositoryProvider(String url, Log log) {
		this.url = url;
		this.log = log;
	}
	
	/**
	 * uploads all pact files to a git repo.
	 * using following file structure:
	 * *.git/provider/consumer/provider-consumer.json
	 */
	@Override
	public void uploadPacts(List<PactFile> pacts) throws Exception {
		log.info("using pact repository: " + url);
		File repoDir = new File(path);
		GitApi repository = initRepository(url, repoDir);
		
		copyPactsToRepository(repoDir, pacts);
		boolean changesPushed = repository.pushChanges("pact commit");
		if (changesPushed)
			log.info("changed pacts pushed");
		else
			log.info("no pacts changed. push skipped");
	}

	/**
	 * download all pact files from a git repo.
	 * using following file structure:
	 * *.git/provider/consumer/provider-consumer.json
	 */
	@Override
	@SuppressWarnings("unused")
	public void downloadPacts(String providerId, File targetDirectory) throws Exception {
		log.info("using pact repository: " + url);
		File repoDir = new File(path);
		GitApi repository = initRepository(url, repoDir);
		copyPactsFromRepository(repoDir, providerId, targetDirectory);
	}
	
	
	
	/**
	 * copies files form  target/pact-git-temp/provider/consumer/*.json to target/pacts/'consumer'-'provider'.json
	 */
	private void copyPactsFromRepository(File repoDir, String providerId, File targetDirectory) throws Exception {
		Path providerPath = new File(repoDir, providerId).toPath();
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.json");
		Files.walkFileTree(providerPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (matcher.matches(file)){
					PactFile pact = PactFile.readPactFile(file.toFile());
					Path targetFile = Paths.get(targetDirectory.getAbsolutePath(), 
							pact.getConsumer() + "-" +pact.getProvider() + ".json");
					targetFile.toFile().mkdirs();
					Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);	
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private GitApi initRepository(String url, File repoDir) throws Exception {
		if (!repoDir.exists())
			repoDir.mkdirs();
		
		GitApi repository = new GitApi();
		repository.init(repoDir, url);
		return repository;
	}
	
	/**
	 * copies files form target/pacts/*.json to target/pact-git-temp/provider/consumer/*.json
	 */
	private void copyPactsToRepository(File repoDir, List<PactFile> pacts) throws Exception {
		log.debug("copying files to repository");
		for (PactFile pact : pacts) {
			File file = new File(pact.getPath());
			Path targetFile = Paths.get(repoDir.getAbsolutePath(), 
										pact.getProvider(), 
										pact.getConsumer(), 
										file.getName());
			targetFile.toFile().mkdirs();
			Files.copy(file.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING);
		}
	}


	

	
}
