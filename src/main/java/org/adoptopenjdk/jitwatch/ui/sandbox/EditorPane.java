/*
 * Copyright (c) 2013, 2014 Chris Newland.
 * Licensed under https://github.com/AdoptOpenJDK/jitwatch/blob/master/LICENSE-BSD
 * Instructions: https://github.com/AdoptOpenJDK/jitwatch/wiki
 */
package org.adoptopenjdk.jitwatch.ui.sandbox;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.adoptopenjdk.jitwatch.loader.ResourceLoader;
import org.adoptopenjdk.jitwatch.sandbox.Sandbox;
import org.adoptopenjdk.jitwatch.ui.Dialogs;
import org.adoptopenjdk.jitwatch.ui.Dialogs.Response;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyleSpans;
import org.fxmisc.richtext.StyleSpansBuilder;

public class EditorPane extends VBox
{

	private static final String PAREN_PATTERN = "\\(|\\)";
	private static final String BRACE_PATTERN = "\\{|\\}";
	private static final String BRACKET_PATTERN = "\\[|\\]";
	private static final String SEMICOLON_PATTERN = "\\;";
	private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
	private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";

	private static final String[] KEYWORDS = new String[] {
			"abstract", "assert", "boolean", "break", "byte",
			"case", "catch", "char", "class", "const",
			"continue", "default", "do", "double", "else",
			"enum", "extends", "final", "finally", "float",
			"for", "goto", "if", "implements", "import",
			"instanceof", "int", "interface", "long", "native",
			"new", "package", "private", "protected", "public",
			"return", "short", "static", "strictfp", "super",
			"switch", "synchronized", "this", "throw", "throws",
			"transient", "try", "void", "volatile", "while"
	};


	private final Pattern highlightPattern;
	private final CodeArea codeArea;
	private final ISandboxStage sandboxStage;

	private boolean isModified = false;

	private File sourceFile = null;

	public EditorPane(ISandboxStage stage)
	{
		this.sandboxStage = stage;

		StringBuilder keywords = new StringBuilder();
		for(String keyword : KEYWORDS)
		{
			keywords.append(keyword).append("|");
		}

		String keywordPattern = "\\b(" + keywords.substring(0,keywords.length()-1) + ")\\b";

		Pattern pattern = Pattern.compile(""
						+ "(?<KEYWORD>" + keywordPattern + ")"
						+ "|(?<PAREN>" + PAREN_PATTERN + ")"
						+ "|(?<BRACE>" + BRACE_PATTERN + ")"
						+ "|(?<BRACKET>" + BRACKET_PATTERN + ")"
						+ "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
						+ "|(?<STRING>" + STRING_PATTERN + ")"
						+ "|(?<COMMENT>" + COMMENT_PATTERN + ")"
		);

		this.highlightPattern = pattern;

		this.codeArea = createCodeArea();

		getChildren().add(codeArea);

	}

	private CodeArea createCodeArea()
	{
		CodeArea codeArea = new CodeArea();

		codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

		codeArea.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> obs, String oldText, String newText) {
				codeArea.setStyleSpans(0, computeHighlighting(newText));
			}
		});

		codeArea.getStylesheets().add(getClass().getClassLoader().getResource("java-syntax-color.css").toExternalForm());
		codeArea.prefHeightProperty().bind(heightProperty());

		return codeArea;
	}

	private StyleSpans<Collection<String>> computeHighlighting(String text) {

		Matcher matcher = this.highlightPattern.matcher(text);

		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

		while (matcher.find()) {

			String styleClass = null;

			if (matcher.group("KEYWORD") != null) {
				styleClass = "keyword";
			} else if (matcher.group("PAREN") != null) {
				styleClass = "paren";
			} else if (matcher.group("BRACE") != null) {
				styleClass = "brace";
			} else if (matcher.group("BRACKET") != null) {
				styleClass = "bracket";
			} else if (matcher.group("SEMICOLON") != null) {
				styleClass = "semicolon";
			} else if (matcher.group("STRING") != null) {
				styleClass = "string";
			} else if (matcher.group("COMMENT") != null) {
				styleClass = "comment";
			} else {
				styleClass = null;
			}

			assert styleClass != null;

			spansBuilder.add(Collections.<String>emptyList(), matcher.start() - lastKwEnd);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastKwEnd = matcher.end();
		}

		spansBuilder.add(Collections.<String>emptyList(), text.length() - lastKwEnd);
		return spansBuilder.create();
	}

	private void setModified(final boolean modified)
	{
		this.isModified = modified;
		sandboxStage.setModified(this, isModified);
	}

	public boolean isModified()
	{
		return isModified;
	}

	public String getSource()
	{
		return codeArea.getText().trim();
	}

	public File getSourceFile()
	{
		return sourceFile;
	}

	public String getName()
	{
		String result;

		if (sourceFile == null)
		{
			result = "New";
		}
		else
		{
			result = sourceFile.getName();
		}

		return result;
	}

	public void loadSource(File filename)
	{
		sourceFile = filename;

		if (sourceFile != null)
		{
			// add parent folder so source can be loaded in TriView
			sandboxStage.addSourceFolder(sourceFile.getParentFile());

			String source = ResourceLoader.readFile(sourceFile);

			if (source != null)
			{
				source = source.replace("\t", "    ");

				codeArea.replaceText(0, 0, source.trim());

				setModified(false);
			}
		}
	}

	public void promptSave()
	{
		if (isModified)
		{
			Response resp = Dialogs.showYesNoDialog(sandboxStage.getStageForChooser(), "Save modified file?", "Save changes?");

			if (resp == Response.YES)
			{
				saveFile();
			}
		}
	}

	public void saveFile()
	{
		if (sourceFile == null)
		{
			FileChooser fc = new FileChooser();
			fc.setTitle("Save file as");

			fc.setInitialDirectory(Sandbox.SANDBOX_SOURCE_DIR.toFile());

			sourceFile = fc.showSaveDialog(sandboxStage.getStageForChooser());
		}

		if (sourceFile != null)
		{
			saveFile(sourceFile);
			setModified(false);
		}
	}

	private void saveFile(File saveFile)
	{
		FileWriter writer = null;

		try
		{
			writer = new FileWriter(saveFile);
			writer.write(getSource());
			sandboxStage.log("Saved " + saveFile.getCanonicalPath());
		}
		catch (IOException ioe)
		{
			sandboxStage.log("Could not save file");
		}
		finally
		{
			if (writer != null)
			{
				try
				{
					writer.close();
				}
				catch (IOException ioe)
				{
				}
			}
		}
	}
}