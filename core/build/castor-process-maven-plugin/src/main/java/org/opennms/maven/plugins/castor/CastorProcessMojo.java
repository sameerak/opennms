package org.opennms.maven.plugins.castor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

import com.twmacinta.util.MD5;

/**
 * @goal generate
 * @phase generate-sources
 * @description Post-process Castor-generated classes.
 * @author Benjamin Reed <ranger@opennms.org>
 */
public class CastorProcessMojo extends AbstractMojo {
    /**
     * The directory to output the generated sources to
     * 
     * @parameter expression="${project.build.directory}/generated-sources/castor"
     */
    private String dest;

    /**
     * Directory containing the build files.
     * @parameter expression="${project.build.directory}"
     */
    private File buildDirectory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final File topDir = new File(dest);
		
		if (!topDir.exists() || !topDir.isDirectory()) {
			getLog().warn(topDir + " does not exist, or is not a directory, skipping");
			return;
		}

		final Map<Pattern,String> patterns = new HashMap<Pattern,String>();
		patterns.put(Pattern.compile("java\\.lang\\.Class getJavaClass"), "java.lang.Class<?> getJavaClass");
		patterns.put(Pattern.compile("^public class "), "@SuppressWarnings(\"all\") public class ");
		patterns.put(Pattern.compile("^public abstract class "), "@SuppressWarnings(\"all\") public abstract class ");
		patterns.put(Pattern.compile("^.SuppressWarnings.\"serial\"."), "");
		
		recursiveSubst(topDir, patterns);
	}

	private void recursiveSubst(final File dir, final Map<Pattern, String> patterns) throws MojoExecutionException, MojoFailureException {
		for (final File file : dir.listFiles()) {
			if (file.isDirectory()) {
				recursiveSubst(file, patterns);
			} else if (file.getName().endsWith(".java")) {
				subst(file, patterns);
			}
		}
	}

	private void subst(final File source, final Map<Pattern,String> substitutions) throws MojoExecutionException, MojoFailureException {
		final File workDir = new File(buildDirectory, "opennms-castor-process");
		if (!workDir.exists()) {
			if (!workDir.mkdirs()) {
				throw new MojoExecutionException("unable to create directory " + workDir);
			}
		}
		final File tempFile;
		try {
			tempFile = File.createTempFile("subst", "java.tmp", workDir);
			FileUtils.copyFile(source, tempFile);
		} catch (final IOException e) {
			throw new MojoExecutionException("Unable to create temporary file.", e);
		}
		
		FileReader fr = null;
		BufferedReader br = null;
		FileWriter out = null;
		
		try {
			fr = new FileReader(source);
			br = new BufferedReader(fr);
			out = new FileWriter(tempFile);
			
			String line = null;
			while ((line = br.readLine()) != null) {
				for (final Pattern p : substitutions.keySet()) {
					final Matcher m = p.matcher(line);
					line = m.replaceAll(substitutions.get(p));
				}
				out.write(line);
				out.write("\n");
			}
		} catch (final Exception e) {
			throw new MojoFailureException("Unable to filter file contents.", e);
		} finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(br);
			IOUtils.closeQuietly(fr);
		}

		try {
			final String before = MD5.asHex(MD5.getHash(source));
			final String after = MD5.asHex(MD5.getHash(tempFile));
	
			if (before != after) {
				FileUtils.copyFile(tempFile, source);
			} else {
				getLog().debug("Skipping " + source + ": file is unchanged.");
			}
		} catch (final Exception e) {
			throw new MojoFailureException("Unable to compare modified file to original.", e);
		}

		tempFile.delete();
	}
}
