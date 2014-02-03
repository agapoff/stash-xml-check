package com.atlassian.stash.plugin;

import com.atlassian.stash.content.Changeset;
import com.atlassian.stash.history.HistoryService;

import com.atlassian.stash.hook.*;
import com.atlassian.stash.hook.repository.*;
import com.atlassian.stash.repository.*;
import java.util.Collection;

import com.atlassian.stash.scm.git.GitCommandBuilderFactory;
import com.atlassian.stash.scm.git.GitScm;
import com.atlassian.stash.scm.git.GitScmCommandBuilder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import java.io.StringReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;


public class XMLSanityCheck implements PreReceiveRepositoryHook
{
    private final HistoryService historyService;
    private final GitScm gitScm;

    public XMLSanityCheck(HistoryService historyService, GitScm gitScm) {
        this.historyService = historyService;
        this.gitScm = gitScm;
    }


    @Override
    public boolean onReceive(RepositoryHookContext context, Collection<RefChange> refChanges, HookResponse hookResponse)
    {
	Repository repository = context.getRepository();
        for (RefChange refChange : refChanges)
        {
		String result = gitScm.getCommandBuilderFactory().builder(repository)
                .command("diff")
                .argument("--name-only")
                .argument(refChange.getFromHash())
		.argument(refChange.getToHash())
                .build(new StringCommandOutputHandler())
                .call(); 

		//hookResponse.out().println(result);

		String[] files = result.split("\n");

		for (String file : files) {
			if (file.matches(".+\\.xml$")) {
				//hookResponse.out().println("+" + file);
				if ( checkFile(refChange,repository,hookResponse,file) ) return false;
			}
		}
        }
	hookResponse.out().println("XML syntax check PASSED");
        return true;
    }

	private boolean checkFile(RefChange refChange, Repository repository, HookResponse hookResponse, String file) {
		String result = gitScm.getCommandBuilderFactory().builder(repository)
		.command("show")
		.argument(refChange.getToHash() + ":" + file)
		.build(new StringCommandOutputHandler())
		.call();
		
		//hookResponse.out().println(result);

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(true);

			DocumentBuilder builder = factory.newDocumentBuilder();

			Document document = builder.parse(new InputSource(new StringReader(result)));
		} catch (ParserConfigurationException e) {
		            hookResponse.out().println(e);
			    return true;
	        } catch (SAXException e) {
		            hookResponse.out().println(file + " XML bad syntax: " + e);
			    return true;
	        } catch (IOException e) {
		            hookResponse.out().println(e);
			    return true;
        	}
		return false;
	}
}
