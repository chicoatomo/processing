#ifdef EDITOR

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;


public class PdeEditor extends Panel {

  static final String DEFAULT_PROGRAM = "// type program here\n";

  static final String NEW_SKETCH_ITEM = "( new sketch )";
  static final String SKETCH_PREFIX_NAME = "sketch-";
  static final String CODE_FILENAME = "sketch.pde";

  // otherwise, if the window is resized with the message label
  // set to blank, it's preferredSize() will be fukered
  static final String EMPTY = "                                                                                                                                                             ";
  //PdeBase app;

  PdeEditorButtons buttons;
  PdeEditorHeader header;
  PdeEditorStatus status;
  PdeEditorConsole console;
  TextArea textarea;

  PdeRunner runner;

  Frame frame;
  Window fullScreenWindow;

  static final int GRID_SIZE  = 33;
  static final int INSET_SIZE = 5;

  String lastDirectory;
  String lastFile;

  boolean playing;


  public PdeEditor(/*PdeBase app,*/ String program) {
    //this.app = app;

    setLayout(new BorderLayout());

    Panel leftPanel = new Panel();
    leftPanel.setLayout(new BorderLayout());

    // set bgcolor of buttons here, b/c also used for empty component
    buttons = new PdeEditorButtons(this);
    Color buttonBgColor = 
      PdeBase.getColor("editor.buttons.bgcolor", new Color(153, 0, 0));
    buttons.setBackground(buttonBgColor);
    leftPanel.add("North", buttons);
    Label dummy = new Label();
    dummy.setBackground(buttonBgColor);
    leftPanel.add("Center", dummy);

    add("West", leftPanel);

    Panel rightPanel = new Panel();
    rightPanel.setLayout(new BorderLayout());

    header = new PdeEditorHeader(this, "untitled", "default");
    rightPanel.add("North", header);

    if (program == null) program = DEFAULT_PROGRAM;
    textarea = 
      new TextArea(program, 
		   PdeBase.getInteger("editor.program.rows", 20),
		   PdeBase.getInteger("editor.program.columns", 60),
		   TextArea.SCROLLBARS_VERTICAL_ONLY);
    textarea.setFont(PdeBase.getFont("editor.program.font",
				       new Font("Monospaced", 
						Font.PLAIN, 12)));
    rightPanel.add("Center", textarea);

    Panel statusPanel = new Panel();
    statusPanel.setLayout(new BorderLayout());
    status = new PdeEditorStatus(this);
    statusPanel.add("North", status);
    console = new PdeEditorConsole(this);
    statusPanel.add("South", console);
    rightPanel.add("South", statusPanel);

    add("Center", rightPanel);

    if (!PdeBase.isMacintosh()) {  // this still relevant?
      PdeEditorListener listener = new PdeEditorListener();
      textarea.addKeyListener(listener);
      textarea.addFocusListener(listener);
      textarea.addKeyListener(new PdeKeyListener(this));
    }

    runner = new PdeRunner(this);
  }


  public void doPlay() {
    //doStop();
    doClose();
    playing = true;
    buttons.play();

    runner.setProgram(textarea.getText());
    runner.start();

    // required so that key events go to the panel and <key> works
    //graphics.requestFocus();  // removed for pde
  }


#ifdef RECORDER
  public void doRecord() {
    //doStop();
    doClose();
    PdeRecorder.start(this, graphics.width, graphics.height);
    doPlay();
  }
#endif


  public void doStop() {
#ifdef RECORDER
    if (!playing) return;
#endif
    terminate();
    buttons.clear();
    playing = false;
  }


  public void doClose() {
    if (playing) {
      //System.out.println("was playing, will call doStop()");
      doStop();
    }

    // some code to close the window here
    try {
      // runner.engine is null (runner is not)
      ((KjcEngine)(runner.engine)).close();
      // runner shouldn't be set to null because it gets reused
      //System.err.println("runner = " + runner);
      //runner = null;
    } catch (Exception e) { }
    buttons.clear();
  }


  /*
  public void doOpen(Component comp, int compX, int compY) {
    // first get users/top-level entries in sketchbook
    try {
      File sketchbookDir = new File("sketchbook");
      String toplevel[] = sketchbookDir.list();

      PopupMenu menu = new PopupMenu();

      menu.add(NEW_SKETCH_ITEM);
      menu.addSeparator();

      // header knows what the current user is
      for (int i = 0; i < toplevel; i++) {
	if ((toplevel[i].equals(header.user)) ||
	    (toplevel[i].equals(".")) ||
	    (toplevel[i].equals(".."))) continue;

	Menu submenu = new Menu(toplevel[i]);
	File subdir = new File(sketchbookDir, toplevel[i]);

	String path = subdir.getCanonicalPath();
	submenu.addActionListener(new OpenMenuListener(this, path));
	//submenu.addActionListener(new ActionAdapter() {
	//});

	String entries[] = subdir.list();
	for (int j = 0; j < entries.length; j++) {
	  if ((entries[j].equals(".")) || 
	      (entries[j].equals(".."))) continue;
	  submenu.add(entries[j]);
	}

	menu.add(submenu);
      }
      menu.addSeparator();

      // this might trigger even if a submenu isn't selected, 
      // but hopefully not
      String mypath = path + File.separator + header.user;
      menu.addActionListener(new OpenMenuListener(this, mypath));

      String entries[] = new File(mypath).list();
      for (int j = 0; j < entries.length; j++) {
	if ((entries[j].equals(".")) || 
	    (entries[j].equals(".."))) continue;
	submenu.add(entries[j]);
      }      

      // show the feller and get psyched for a selection
      menu.show(comp, compX, compY);

    } catch (IOException e) {
      e.printStackTrace();
    }
    buttons.clear();
  }
  */


  public void skNew() {
    try {
      // does all the plumbing to create a new project
      // then calls handleOpen to load it up

      File sketchbookDir = new File("sketchbook", header.user);
      File sketchDir = null;
      String sketchName = null;
System.out.println("1");
      do {
	int index = (int) (Math.random() * 1000);
	sketchName = "sketch-" + pad3(index);
	sketchDir = new File(sketchbookDir, sketchName);
      } while (sketchDir.exists());
System.out.println("2");

      // mkdir for new project name
      sketchDir.mkdirs();
      new File(sketchDir, "data").mkdirs();
      new File(sketchDir, "build").mkdirs();
System.out.println("3");

      // make empty pde file
      File sketchFile = new File(sketchDir, sketchName + ".pde");
      new FileOutputStream(sketchFile);

      // make 'data' 'applet' dirs inside that
      // actually, don't, that way can avoid too much extra mess

      // now open it up
      //skOpen(sketchFile, sketchDir);
      handleOpen(sketchFile, sketchDir);

    } catch (IOException e) {
      // NEED TO DO SOME ERROR REPORTING HERE ***
      e.printStackTrace();
    }
  }

  static String pad3(int what) {
    if (what < 10) return "000" + what;
    else if (what < 100) return "00" + what;
    else return String.valueOf(what);
  }

  /*
  static String pad4(int what) {
    if (what < 10) return "000" + what;
    else if (what < 100) return "00" + what;
    else if (what < 1000) return "0" + what;
    else return String.valueOf(what);
  }
  */

  public void skOpen(String path, String name) {
    //header.isProject = true;
    //header.project = name;
    handleOpen(new File(path + File.separator + name, name + ".pde"), 
	       new File(path));
  }


  public void doOpen() {
    FileDialog fd = new FileDialog(new Frame(), 
				   "Open a PDE program...", 
				   FileDialog.LOAD);
    fd.setDirectory(lastDirectory);
    //fd.setFile(lastFile);
    fd.show();

    String directory = fd.getDirectory();
    String filename = fd.getFile();
    if (filename == null) {
      buttons.clear();
      return; // user cancelled
    }

    //header.isProject = false;
    //header.project = filename;
    handleOpen(new File(directory, filename), null);
  }


  protected void handleOpen(File file, File projectDir) {
    //File file = new File(directory, filename);

    try {
      FileInputStream input = new FileInputStream(file);
      int length = (int) file.length();
      byte data[] = new byte[length];

      int count = 0;
      while (count != length) {
	data[count++] = (byte) input.read();
      }
      // set the last dir and file, so that they're
      // the defaults when you try to save again
      lastDirectory = file.getCanonicalPath(); //directory;
      lastFile = file.getName(); //filename;

      // once read all the bytes, convert it to the proper
      // local encoding for this system.
      //textarea.setText(app.languageEncode(data));
      // what the hell was i thinking when i wrote this code
      //if (app.encoding == null)
      textarea.setText(new String(data));
      //else 
      //textarea.setText(new String(data, app.encoding));

      header.setProject(file.getName(), projectDir);

    } catch (FileNotFoundException e1) {
      e1.printStackTrace();
	
    } catch (IOException e2) {
      e2.printStackTrace();
    }
    buttons.clear();
  }


  public void doSave() {
    // true if lastfile not set, otherwise false, meaning no prompt
    handleSave(lastFile == null);
  }

  public void doSaveAs() {
    handleSave(true);
  }

  protected void handleSave(boolean promptUser) {
    message("Saving file...");
    String s = textarea.getText();

    String directory = lastDirectory;
    String filename = lastFile;

System.out.println("1");
    if (promptUser) {
      FileDialog fd = new FileDialog(new Frame(), 
				     "Save PDE program as...", 
				     FileDialog.SAVE);
      fd.setDirectory(lastDirectory);
      fd.setFile(lastFile);
      fd.show();

      directory = fd.getDirectory();
      filename = fd.getFile();
      if (filename == null) {
	message(EMPTY);
	buttons.clear();
	return; // user cancelled
      }
    }
    File file = new File(directory, filename);
System.out.println("2");

    try {
      FileWriter writer = new FileWriter(file);
      writer.write(s);
      writer.flush();
      writer.close();
System.out.println("3");
      lastDirectory = directory;
      lastFile = filename;
      message("Done saving " + filename + ".");

    } catch (IOException e) {
      e.printStackTrace();
      //message("Did not write file.");
      message("Could not write " + filename + ".");
    }
    buttons.clear();
  }


  public void skDuplicate() {
    System.err.println("sketch duplicate not yet implemented");
  }


  public void skExport() {
    File appletDir = new File(header.projectDir, "applet");
    handleExport(appletDir, header.project, 
		 new File(header.projectDir, "data"));
  }

  public void doExport() {
    message("Exporting to applet...");
    String s = textarea.getText();
    FileDialog fd = new FileDialog(new Frame(), 
				   "Create applet project named...", 
				   FileDialog.SAVE);
    fd.setDirectory(lastDirectory);
    fd.setFile(lastFile);
    fd.show();

    String directory = fd.getDirectory();
    String projectName = fd.getFile();
    if (projectName == null) {   // user cancelled
      message(EMPTY);
      buttons.clear();
      return;
    } else if (projectName.indexOf(' ') != -1) {  // space in filename
      message("Project name cannot have spaces.");
      buttons.clear();
      return;
    }

    handleExport(new File(directory), projectName, null);
  }

  protected void handleExport(File appletDir, String projectName, 
			      File dataDir) {
    try {
      String program = textarea.getText();

      // create the project directory
      KjcEngine engine = new KjcEngine(program, this);
      //File projectDir = new File(appletDir, projectName);
      //projectDir.mkdirs();
      appletDir.mkdirs();

      // projectName will be updated with actual class name
      projectName = engine.writeJava(projectName, false);
      if (!engine.compileJava()) {
	//throw new Exception("error while compiling, couldn't export");
	// message() will already have error message in this case
	return;
      }

      // copy .java to project dir
      String javaName = projectName + ".java";
      //copyFile(new File(javaName), new File(projectDir, javaName));
      copyFile(new File(javaName), new File(appletDir, javaName));

      // remove temporary .java and .class files
      //engine.cleanup();

      int wide = BApplet.DEFAULT_WIDTH;
      int high = BApplet.DEFAULT_HEIGHT;

      int index = program.indexOf("size(");
      if (index != -1) {
	try {
	  String str = program.substring(index + 5);
	  int comma = str.indexOf(',');
	  int paren = str.indexOf(')');
	  wide = Integer.parseInt(str.substring(0, comma).trim());
	  high = Integer.parseInt(str.substring(comma+1, paren).trim());
	} catch (Exception e) { 
	  e.printStackTrace();
	}
      }

      //File htmlOutputFile = new File(projectDir, "index.html");
      File htmlOutputFile = new File(appletDir, "index.html");
      FileOutputStream fos = new FileOutputStream(htmlOutputFile);
      PrintStream ps = new PrintStream(fos);
      ps.println("<HTML> <BODY BGCOLOR=\"white\">");
      ps.println();
      ps.println("<BR> <BR> <BR> <CENTER>");

      ps.println();
      ps.print("<APPLET CODE=\"" + projectName  + "\" ARCHIVE=\"");
      ps.print(projectName + ".jar");
      ps.println("\" WIDTH=" + wide + " HEIGHT=" + high + ">");
      ps.println("</APPLET>");
      ps.println();

      ps.println("<A HREF=\"" + projectName + ".java\">source code</A>");
      ps.println();

      ps.println("</CENTER>");

      ps.println("</BODY> </HTML>");
      ps.flush();
      ps.close();

      String exportDir = ("lib" + File.separator + 
			  "export" + File.separator);
      String bagelClasses[] = new File(exportDir).list();

      // create new .jar file
      FileOutputStream zipOutputFile = 
	new FileOutputStream(new File(appletDir, projectName + ".jar"));
	//new FileOutputStream(new File(projectDir, projectName + ".jar"));
      ZipOutputStream zos = new ZipOutputStream(zipOutputFile);
      ZipEntry entry;

      // add standard .class files to the jar
      for (int i = 0; i < bagelClasses.length; i++) {
	if (!bagelClasses[i].endsWith(".class")) continue;
	entry = new ZipEntry(bagelClasses[i]);
	zos.putNextEntry(entry);
	zos.write(grabFile(new File(exportDir + bagelClasses[i])));
	zos.closeEntry();
      }

      // files to include
      if (dataDir != null) {
	String datafiles[] = dataDir.list();
	for (int i = 0; i < datafiles.length; i++) {
	  if (datafiles[i].equals(".") || datafiles[i].equals("..")) {
	    continue;
	  }
	  entry = new ZipEntry(datafiles[i]);
	  zos.putNextEntry(entry);
	  zos.write(grabFile(new File(dataDir, datafiles[i])));
	  zos.closeEntry();
	}
      }

      // add the project's .class to the jar
      entry = new ZipEntry(projectName + ".class");
      zos.putNextEntry(entry);
      zos.write(grabFile(new File("lib", projectName + ".class")));
      zos.closeEntry();

      // close up the jar file
      zos.flush();
      zos.close();
      //zipOutputFile.close();

      engine.cleanup();

      message("Done exporting.");

    } catch (Exception e) {
      message("Error during export.");
      e.printStackTrace();
    }
    buttons.clear();
  }


  public void doPrint() {
    /*
    Frame frame = new Frame(); // bullocks
    int screenWidth = getToolkit().getScreenSize().width;
    frame.reshape(screenWidth + 20, 100, screenWidth + 100, 200);
    frame.show();

    Properties props = new Properties();
    PrintJob pj = getToolkit().getPrintJob(frame, "PDE", props);
    if (pj != null) {
      Graphics g = pj.getGraphics();
      // awful way to do printing, but sometimes brute force is
      // just the way. java printing across multiple platforms is
      // outrageously inconsistent.
      int offsetX = 100;
      int offsetY = 100;
      int index = 0;
      for (int y = 0; y < graphics.height; y++) {
	for (int x = 0; x < graphics.width; x++) {
	  g.setColor(new Color(graphics.pixels[index++]));
	  g.drawLine(offsetX + x, offsetY + y,
		     offsetX + x, offsetY + y);
	}
      }
      g.dispose();
      g = null;
      pj.end();
    }
    frame.dispose();
    buttons.clear();
    */
  }


  public void doQuit() {
    System.exit(0);
  }


  public void doBeautify() {
    String prog = textarea.getText();
    //if ((prog.charAt(0) == '#') || (prog.charAt(0) == ';')) {
    //message("Only DBN code can be made beautiful.");
    //buttons.clear();
    //return;
    //}
    char program[] = prog.toCharArray();
    StringBuffer buffer = new StringBuffer();
    boolean gotBlankLine = false;
    int index = 0;
    int level = 0;

    while (index != program.length) {
      int begin = index;
      while ((program[index] != '\n') &&
	     (program[index] != '\r')) {
	index++;
	if (program.length == index)
	  break;
      }
      int end = index;
      if (index != program.length) {
	if ((index+1 != program.length) &&
	    // treat \r\n from windows as one line
	    (program[index] == '\r') && 
	    (program[index+1] == '\n')) {
	  index += 2;
	} else {
	  index++;
	}		
      } // otherwise don't increment

      String line = new String(program, begin, end-begin);
      line = line.trim();
	    
      if (line.length() == 0) {
	if (!gotBlankLine) {
	  // let first blank line through
	  buffer.append('\n');
	  gotBlankLine = true;
	}
      } else {
	System.out.println(level);
	int idx = -1;
	String myline = line.substring(0);
	while (myline.lastIndexOf('}') != idx) {
	  idx = myline.indexOf('}');
	  myline = myline.substring(idx+1);
	  level--;
	}
	for (int i = 0; i < level*2; i++) {
	  buffer.append(' ');
	}
	buffer.append(line);
	buffer.append('\n');
	//if (line.charAt(0) == '{') {
	//level++;
	//}
	idx = -1;
	myline = line.substring(0);
	while (myline.lastIndexOf('{') != idx) {
	  idx = myline.indexOf('{');
	  myline = myline.substring(idx+1);
	  level++;
	}
	gotBlankLine = false;
      }
    }
    textarea.setText(buffer.toString());
    buttons.clear();
  }


  public void enableFullScreen() {
    if (fullScreenWindow == null) {
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      if (PdeBase.isMacintosh()) {
	fullScreenWindow = new Frame();

	// mrj is still (with version 2.2.x) a piece of shit, 
	// and doesn't return valid insets for frames
	//fullScreenWindow.pack(); // make a peer so insets are valid
	//Insets insets = fullScreenWindow.getInsets();
	// the extra +20 is because the resize boxes intrude
	Insets insets = new Insets(21, 5, 5 + 20, 5);
	//System.out.println(insets);

	fullScreenWindow.setBounds(-insets.left, -insets.top, 
				   screen.width + insets.left + insets.right, 
				   screen.height + insets.top + insets.bottom);
      } else {
	fullScreenWindow = new Window(new Frame());
	fullScreenWindow.setBounds(0, 0, screen.width, screen.height);
      }
      Color fullScreenBgColor = 
	PdeBase.getColor("fullscreen.bgcolor", new Color(102, 102, 102));
      fullScreenWindow.setBackground(fullScreenBgColor);

      /*
      fullScreenWindow.addWindowListener(new WindowAdapter() {
	  public void windowActivated(WindowEvent e) {
	    System.out.println("activated");
	  }
	}
      );
      */
      fullScreenWindow.addFocusListener(new FocusAdapter() {
	  public void focusGained(FocusEvent e) {
	    //System.out.println("activated");
	    /*PdeApplication.*/ 
	    if (frame != null) frame.toFront();
	  }
	}
      );
    }
    fullScreenWindow.show();
    fullScreenWindow.toFront();

    // not sure what to do with applet..
    // (since i can't bring the browser window to the front)
    // unless there's a method in AppletContext
    //if (frame != null) frame.toFront();

    /*
    try {
      //System.out.println("my parent is " + getParent());
      ((PdeApplication)getParent()).frame.toFront();
    } catch (Exception e) { }
    */

    try {
      ((KjcEngine)(runner.engine)).window.toFront();
    } catch (Exception e) {
      // rather than writing code to check all the posible
      // errors with the above statement, just fail quietly
      //System.out.println("couldn't bring kjc engine window forward");
    }
    //if (runner.engine != null) {
    //if (runner.engine instanceof KjcEngine) {	
    //}
    //}

    buttons.clear();
  }

  public void disableFullScreen() {
    fullScreenWindow.hide();
    buttons.clear();
  }


  public void terminate() {   // part of PdeEnvironment
    runner.stop();
    message(EMPTY);
  }


  // TODO iron out bugs with this code under
  //      different platforms, especially macintosh
  public void highlightLine(int lnum) {
    if (lnum < 0) {
      textarea.select(0, 0);
      return;
    }
    //System.out.println(lnum);
    String s = textarea.getText();
    int len = s.length();
    //int lnum = .line;
    int st = -1, end = -1;
    int lc = 0;
    if (lnum == 0) st = 0;
    for (int i = 0; i < len; i++) {
      //if ((s.charAt(i) == '\n') || (s.charAt(i) == '\r')) {
      boolean newline = false;
      if (s.charAt(i) == '\r') {
	if ((i != len-1) && (s.charAt(i+1) == '\n')) i++;
	lc++;
	newline = true;
      } else if (s.charAt(i) == '\n') {
	lc++;
	newline = true;
      }
      if (newline) {
	if (lc == lnum)
	  st = i+1;
	else if (lc == lnum+1) {
	  end = i;
	  break;
	}
      }
    }
    if (end == -1) end = len;
    //System.out.println("st/end: "+st+"/"+end);
    textarea.select(st, end+1);
    //if (iexplorerp) {
    //textarea.invalidate();
    //textarea.repaint();
    //}
  }


  public void error(PdeException e) {   // part of PdeEnvironment
    if (e.line >= 0) highlightLine(e.line); 
    //dbcp.repaint(); // button should go back to 'play'
    //System.err.println(e.getMessage());
    //message("Problem: " + e.getMessage());

    status.error(e.getMessage());
    //message(e.getMessage());

    buttons.clearPlay();

    //showStatus(e.getMessage());
  }


  public void finished() {  // part of PdeEnvironment
#ifdef RECORDER
    PdeRecorder.stop();
#endif
    playing = false;
    buttons.clearPlay();
    message("Done.");
  }


  public void message(String msg) {  // part of PdeEnvironment
    //status.setText(msg);
    //System.out.println("PdeEditor.message " + msg);
    status.notice(msg);
  }
  
  
  public void messageClear(String msg) {
    //if (status.getText().equals(msg)) status.setText(EMPTY);
    //System.out.println("PdeEditor.messageClear " + msg);
    status.unnotice(msg);
  }


  // utility functions


  static protected byte[] grabFile(File file) throws IOException {
    int size = (int) file.length();
    FileInputStream input = new FileInputStream(file);
    byte buffer[] = new byte[size];
    int offset = 0;
    int bytesRead;
    while ((bytesRead = input.read(buffer, offset, size-offset)) != -1) {
      offset += bytesRead;
      if (bytesRead == 0) break;
    }
    return buffer;
  }

  static protected void copyFile(File afile, File bfile) {
    try {
      FileInputStream from = new FileInputStream(afile);
      FileOutputStream to = new FileOutputStream(bfile);
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = from.read(buffer)) != -1) {
	to.write(buffer, 0, bytesRead);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}


#endif

