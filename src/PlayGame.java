import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.*;

import java.util.ArrayList;
import java.sql.Connection;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.HashSet;

public class PlayGame extends JFrame implements ActionListener, KeyListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
    private String framework = "embedded";
    private String protocol = "jdbc:derby:";
	ArrayList<QuoteSource> sources;
	String wordsForGod[] = {"Keep original text", "God", "Allah"};
	int wordForGod = 0;
	String reWordsForGod;
	HashMap<Character,PlayerChoice> playerKeys;
	HashMap<PlayerChoice,Character> reversePlayerKeys;
	String answer = "";
	String answerInfo = "";
	int numPlayers = 2;
	ArrayList<Player> players;
	char defaultKeys[][] = {{'q','i'},
							{'a','k'},
							{'z',','},
							{'w','o'},
							{'s','l'},
							{'x','.'}};
	boolean questionAnswered;

	class Player {
		String name;
		int correctGuesses;
		int wrongGuesses;
		
		Player(String name) {
			this.name = name;
			this.correctGuesses = 0;
			this.wrongGuesses = 0;
		}
	}
	
	class PlayerChoice {
		Integer player;
		String choice;
		PlayerChoice(int player, String choice) {
			this.player = player;
			this.choice = choice;
		}

		@Override 
		public boolean equals( Object o) {
			return (o instanceof PlayerChoice && 
					(this.player == ((PlayerChoice)o).player) && 
					(this.choice == ((PlayerChoice)o).choice) ) ;
		}
		@Override 
		public int hashCode() {
			int result =  player.hashCode();
			result = 31 * result + choice.hashCode();
			return result;
		}
	}

	class QuoteSource {
		public String source;
		public String tableName;
		public int quoteColumn;
		public int numQuotes;
		
		QuoteSource(String source, String tableName, int quoteColumn, int numQuotes) {
			this.source = source;
			this.tableName = tableName;
			this.quoteColumn = quoteColumn;
			this.numQuotes = numQuotes;
		}
	
		
	}
	JPanel panel;
	JMenuBar mb;
	JMenu settings;
	JMenuItem setPlayers, setPlayerNames, setPlayerKeys, options;
	ArrayList<OptionButton> buttons = new ArrayList<OptionButton>(); 
	JTextArea quoteText;
	String[] scoreColumns = {"Player", "Correct", "Wrong", "Total", "Average"};
	String[][] scoreData;
	JTable scores;
	DefaultTableModel model;
	JButton next;
	int V_BUTTON_START = 50;
	int V_QUOTES_START;
	PlayGame() {
		super("Which One Is It");
		
		panel = new JPanel();
        panel.setBounds(0,0,600,600);    
        panel.setBackground(Color.gray);
        
        mb = new JMenuBar();
        settings = new JMenu("Settings");
        setPlayers = new JMenuItem("Players");
        setPlayerNames = new JMenuItem("Player Names");
        setPlayerKeys = new JMenuItem("Player Keys");
        options = new JMenuItem("Options");
        setPlayers.addActionListener(this);
        setPlayerNames.addActionListener(this);
        setPlayerKeys.addActionListener(this);
        options.addActionListener(this);
        settings.add(setPlayers);
        settings.add(setPlayerNames);
        settings.add(setPlayerKeys);
        settings.add(options);
        mb.add(settings);
        this.setJMenuBar(mb);;
        
		JLabel instructions = new JLabel("Below are a couple quotes.");
		JLabel instructions2 = new JLabel("Select the source you think it came from.");
		instructions.setBounds(10,10,200,30);
		instructions2.setBounds(10,25,300,30);
		panel.add(instructions); panel.add(instructions2);
		
		
		players = new ArrayList<Player>(2);
		players.add(new Player("Player 1"));
		players.add(new Player("Player 2"));
		sources = new ArrayList<QuoteSource>();
		playerKeys = new HashMap<Character,PlayerChoice>();
		reversePlayerKeys = new HashMap<PlayerChoice,Character>();
		sources.add(new QuoteSource("Bible", "T_KJV", 5, 31103));
		sources.add(new QuoteSource("Quran", "QURAN", 5, 6236));
		for (int i = 1; i < this.wordsForGod.length; i++) {
			reWordsForGod += "|" + wordsForGod[i];
		}

		Action keyPressed = new AbstractAction() {
		    public void actionPerformed(ActionEvent e) {
		    	if (!questionAnswered) {
			    	String cmd = e.getActionCommand();
			    	if (cmd.length() == 1) {
			    		char key = cmd.charAt(0);
				    	if ( playerKeys.containsKey(key) ) {
				    		questionAnswered = true;
				    		setScore(playerKeys.get(key));
				    		next.requestFocusInWindow();
				    	}
			    	}
		    	}
		    }
		}; 
		Action enterPressed = new AbstractAction() {
		    public void actionPerformed(ActionEvent e) {
		    	if (questionAnswered) {
		    		getQuote();
		    		questionAnswered = false;
		    		quoteText.setBackground(Color.WHITE);
		    	}
		    }
		}; 
		panel.getActionMap().put("keyPressed", keyPressed);
		for (int i = 0;i< sources.size();i++) {
			buttons.add(new OptionButton(10, V_BUTTON_START + i*30, 100, 30, sources.get(i).source, this));
			playerKeys.put(defaultKeys[i][0],new PlayerChoice(0,sources.get(i).source));
			playerKeys.put(defaultKeys[i][1],new PlayerChoice(1,sources.get(i).source));
			reversePlayerKeys.put(new PlayerChoice(0,sources.get(i).source),defaultKeys[i][0]);
			reversePlayerKeys.put(new PlayerChoice(1,sources.get(i).source),defaultKeys[i][1]);
			panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(defaultKeys[i][0]), "keyPressed");;
			panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(defaultKeys[i][1]), "keyPressed");;
		}
		
		next = new JButton("Next Quote");
		next.setBounds(200, V_BUTTON_START, 100, 30);
		panel.add(next);
		next.addActionListener(this);
		panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "enterPressed");;
		panel.getActionMap().put("enterPressed", enterPressed);

		
		V_QUOTES_START = sources.size()*30 + V_BUTTON_START;
		
		quoteText = new JTextArea("");
		quoteText.setBounds(10,V_QUOTES_START + 30,350,200);
		quoteText.setLineWrap(true);
		quoteText.setWrapStyleWord(true);
		quoteText.setBackground(Color.WHITE);
		panel.add(quoteText);

		getQuote();

		scoreData = new String[numPlayers][scoreColumns.length];
	// {"Player", "Correct", "Wrong", "Total", "Average"};
		for (int i = 0; i < numPlayers; i++) {
			scoreData[i][0] = this.players.get(i).name;
			scoreData[i][1] = "0";
			scoreData[i][2] = "0";
			scoreData[i][3] = "0";
			scoreData[i][4] = "NA";
		}
		scores = new JTable();
		model = new DefaultTableModel(scoreData,scoreColumns);
	
		scores = new JTable(model);
		JScrollPane scoreScrollPane = new JScrollPane(scores);
		scores.setFillsViewportHeight(false);
		scores.setDefaultEditor(Object.class, null);
		panel.add(scoreScrollPane);
		
		add(panel);
		setSize(600,600);
		setLayout(null);
		setVisible(true);
		
		
		
	}
	
	public void promptNumPlayers() {
		JTextField field1 = new JTextField("" + this.numPlayers);
		JPanel nPPrompt = new JPanel(new GridLayout(0,1));
		nPPrompt.add(new JLabel("Number of players"));
		nPPrompt.add(field1);
		boolean bad = true;
		int numPlrsEntrd = numPlayers;
		while (bad) {
			int result = JOptionPane.showConfirmDialog(null
				, nPPrompt, "Enter the number of players"
				,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
			if (result == JOptionPane.OK_OPTION)  {
				
				try {
					numPlrsEntrd = Integer.parseInt(field1.getText() );
					if (numPlrsEntrd < 1 || numPlrsEntrd > 10 ) {
						throw new NumberFormatException("Num Players OOR") ;
					}
						bad = false;
				}
				catch (NumberFormatException e) {
					if (e.getMessage() == "Num Players OOR") {
						field1.setText(field1.getText() + " <- must be > 0 and < 11");
					}
					else {
						field1.setText(field1.getText() + " <- must be a number");
					}
					bad = true;
				}
					 
			} else {
				break;
			}
			if (!bad) {
				if ( numPlayers == numPlrsEntrd ) 
					return;
				if ( numPlayers < numPlrsEntrd )  {
					for (int i = numPlayers; i < numPlrsEntrd; i++) {
						players.add(new Player("Player " + (i + 1)));
						String[] newscoreData = new String[scoreColumns.length];
						newscoreData[0] = players.get(i).name;
						newscoreData[1] = "0";
						newscoreData[2] = "0";
						newscoreData[3] = "0";
						newscoreData[4] = "NA";
						numPlayers++;
						model.addRow(newscoreData);
						while (!reversePlayerKeys.containsKey(new PlayerChoice(i,sources.get(0).source)))
							promptPlayerKeys(i);
					}
				} else {
					for (int i = numPlayers; i > numPlrsEntrd; i--) {
						players.remove(i - 1);
						//scores.remove(i - 1);
						model.removeRow(i - 1);
						for (int j = 0; j < sources.size(); j++) {
							PlayerChoice pc = new PlayerChoice(i - 1,sources.get(j).source);
							char c = reversePlayerKeys.remove(pc);
							playerKeys.remove(c);
							panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).remove(KeyStroke.getKeyStroke(c));;
						}
						numPlayers--;
					}
					
				}
			}
			 
		}

	}

	public void promptPlayerNames() {
		JPanel nPPrompt = new JPanel(new GridLayout(0,1));
		JTextField names[] = new JTextField[numPlayers];
		for (int i = 0; i < numPlayers; i++) {
			nPPrompt.add(new JLabel("Player " + (i+1) ));
			names[i] = new JTextField(players.get(i).name);
			nPPrompt.add(names[i]);
		}
		int result = JOptionPane.showConfirmDialog(null
			, nPPrompt, "Enter the number of players"
			,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION)  {
			for (int i = 0; i < numPlayers; i++) {
				players.get(i).name = names[i].getText();
				scores.setValueAt(names[i].getText(), i, 0);
			}
			
		}

	}

	public void promptPlayerKeys(int player) {
		JComboBox<String> combo = new JComboBox<String>();
		for (int i = 0; i < players.size(); i++) {
			combo.addItem(players.get(i).name);
		}
		if (player >= 0) {
			combo.setSelectedIndex(player);
			combo.setEditable(false);
			combo.setEnabled(false);
		}
		//JTextField field1 = new JTextField("" + this.numPlayers);
		JPanel nPPrompt = new JPanel(new GridLayout(0,1));
		nPPrompt.add(new JLabel("Select player:"));
		nPPrompt.add(combo);
		ArrayList<JTextField> fields = new ArrayList<JTextField>(sources.size());
		ArrayList<JLabel> labels = new ArrayList<JLabel>(sources.size());
		ArrayList<Character> previousKey = new ArrayList<Character>(sources.size());
		for (int i = 0; i < this.sources.size(); i++) {
			labels.add(i,new JLabel(this.sources.get(i).source));
			if (player < 0) {
				fields.add(i,new JTextField( this.reversePlayerKeys.get(
						new PlayerChoice(0,
							labels.get(i).getText())).toString()));
				previousKey.add(i,fields.get(i).getText().charAt(0));
			}
			else {
				fields.add(i,new JTextField(""));
				
			}
			nPPrompt.add(labels.get(i));
			nPPrompt.add(fields.get(i));
		}
		if (player < 0 ) combo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource().getClass() == JComboBox.class) {
					for (int i = 0; i < fields.size(); i++) {
						fields.get(i).setText(reversePlayerKeys.get(
								new PlayerChoice(combo.getSelectedIndex(),
										labels.get(i).getText())).toString());
						previousKey.set(i,fields.get(i).getText().charAt(0));
					}
				}
			}
		});
		//nPPrompt.add(field1);
		JButton submit = new JButton("Submit");
		submit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				HashSet<Character> newKeysHash = new HashSet<Character>();
				boolean good = true;
					for (int i = 0; i < fields.size(); i++) {
						String text = fields.get(i).getText();
						if (text.length() != 1) {
							fields.get(i).setText( text + " <- must be single character");
							good = false;
						} else if (playerKeys.containsKey(text.charAt(0))
								&& playerKeys.get(text.charAt(0)).player != combo.getSelectedIndex()) {
							fields.get(i).setText( text + " <- key already taken");
							good = false;
						} else {
						//	handle no duplicate new key
							if (newKeysHash.contains(text.charAt(0))) {
								fields.get(i).setText( text + " <- key already taken");
								good = false;
							} else {
								newKeysHash.add(text.charAt(0));
							}

						}

					}
					if (good) {
						ArrayList<PlayerChoice> choices = new ArrayList<PlayerChoice>(sources.size());
						for (int i = 0; i < fields.size(); i++ ) {
							char newChar = fields.get(i).getText().charAt(0);
							//if (previousKey.get(i) == newChar )
							//		continue;
							if (player < 0) {
								choices.add(i,playerKeys.remove(previousKey.get(i)));
								panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).remove(KeyStroke.getKeyStroke(previousKey.get(i)));;
							} else {
								choices.add(i,new PlayerChoice(player,labels.get(i).getText()));
							}
						}
						for (int i = 0; i < fields.size(); i++ ) {
							char newChar = fields.get(i).getText().charAt(0);
							//if (previousKey.get(i) == newChar )
							//		continue;
							playerKeys.put(newChar,choices.get(i));
							if (player < 0) 
								reversePlayerKeys.replace(playerKeys.get(newChar), newChar);
							else
								reversePlayerKeys.put(playerKeys.get(newChar), newChar);
							panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(newChar), "keyPressed");;
							if (player < 0) previousKey.set(i,newChar);
						}
						if (player >= 0) return;
					}
					
			}
		});
		nPPrompt.add(submit);
		nPPrompt.add(new JLabel("OK button does not submit settings"));

		JOptionPane.showMessageDialog( null
			, nPPrompt, "Enter the number of players"
			,JOptionPane.PLAIN_MESSAGE);
		//JOptionPane.showConfirmDialog(null
		//	, nPPrompt, "Enter the number of players"
		//	,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		//if (result == JOptionPane.OK_OPTION)  {
			//System.out.println(combo.getSelectedIndex() + ":" + combo.getSelectedItem());
			//submit.doClick();
			//if (!good && player >= 0) {
			//	promptPlayerKeys(player);
			//}
		//	System.out.println("num players = " + field1.getText());
		//}

	}

	public void promptOptions() {
		JPanel nPPrompt = new JPanel(new GridLayout(0,1));
		nPPrompt.add(new JLabel("Word to use for God:"));
		ArrayList<JRadioButton> rbWordsForGod = new ArrayList<JRadioButton>(wordsForGod.length);
		ButtonGroup bgGod = new ButtonGroup();
		for (int i = 0; i < wordsForGod.length; i++) {
			rbWordsForGod.add( new JRadioButton(wordsForGod[i]));
			if (wordForGod == i) 
				rbWordsForGod.get(i).setSelected(true);
			bgGod.add(rbWordsForGod.get(i));
			nPPrompt.add(rbWordsForGod.get(i));
		}
		int result = JOptionPane.showConfirmDialog(null
			, nPPrompt, "Enter the number of players"
			,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION)  {
			for (wordForGod = 0; wordForGod < rbWordsForGod.size(); wordForGod++)
				if (rbWordsForGod.get(wordForGod).isSelected()) break;
		}

	}
	
	public void setScore(PlayerChoice playerChoice) {
		int player = playerChoice.player;
		String selection = playerChoice.choice;
		if (selection.equals(answer)) {
			players.get(player).correctGuesses++;
			quoteText.setBackground(Color.GREEN);
		} else {
			players.get(player).wrongGuesses++;
			quoteText.setBackground(Color.PINK);
		}
		int correctGuesses = players.get(player).correctGuesses;
		int wrongGuesses = players.get(player).wrongGuesses;

		quoteText.setText(answerInfo);
		scores.setValueAt(String.valueOf(correctGuesses), player, 1);
		scores.setValueAt(String.valueOf(wrongGuesses), player, 2);
		scores.setValueAt(String.valueOf(correctGuesses - wrongGuesses), player, 3);
		scores.setValueAt(String.valueOf(100 * correctGuesses / (correctGuesses + wrongGuesses)), player, 4);
		// {"Player", "Correct", "Wrong", "Total", "Average"};
	}

	public void getQuote() {
		
		int i = (int)(Math.random()*(sources.size()));
		QuoteSource randomSource = sources.get(i);
		answer = randomSource.source;
		int randomRow = (int)(Math.random()*randomSource.numQuotes);
        Connection conn = null;
        ArrayList<Statement> statements = new ArrayList<Statement>(); // list of Statements, PreparedStatements
        Statement s;
        ResultSet rs = null;
        try
        {
            Properties props = new Properties(); // connection properties
            // providing a user name and password is optional in the embedded
            // and derbyclient frameworks
            props.put("user", "user1");
            props.put("password", "user1");
            String dbName = "quotesdb"; // the name of the database
            conn = DriverManager.getConnection(protocol + "quotesdb" );

            System.out.println("Connected to database " + dbName);
            s = conn.createStatement();
            statements.add(s);
            String tableName = randomSource.tableName;
            rs = s.executeQuery(
                    "select * from ( " + 
            		"select ROW_NUMBER() over() AS rownum, " + tableName + ".* from " + tableName +
                    " ) as tmp " + 
                    "where rownum = " + randomRow);
            
            if (rs.next());
            String text = rs.getString(randomSource.quoteColumn + 1);
            if (wordForGod > 0)
            	quoteText.setText(text.replaceAll(reWordsForGod,wordsForGod[wordForGod]));
            else
            	quoteText.setText(text);
            answerInfo = "The source was " + answer + "\n" + text;
            conn.commit();
            
        }
        catch (SQLException sqle)
        {
            printSQLException(sqle);
        } finally {
            // release all open resources to avoid unnecessary memory usage

            // ResultSet
            try {
                if (rs != null) {
                    rs.close();
                    rs = null;
                }
            } catch (SQLException sqle) {
                printSQLException(sqle);
            }

            // Statements and PreparedStatements
            int j = 0;
            while (!statements.isEmpty()) {
                // PreparedStatement extend Statement
                Statement st = (Statement)statements.remove(j);
                try {
                    if (st != null) {
                        st.close();
                        st = null;
                    }
                } catch (SQLException sqle) {
                    printSQLException(sqle);
                }
            }

            //Connection
            try {
                if (conn != null) {
                    conn.close();
                    conn = null;
                }
            } catch (SQLException sqle) {
                printSQLException(sqle);
            }
        }
	}

    public static void printSQLException(SQLException e)
    {
        // Unwraps the entire exception chain to unveil the real cause of the
        // Exception.
        while (e != null)
        {
            System.err.println("\n----- SQLException -----");
            System.err.println("  SQL State:  " + e.getSQLState());
            System.err.println("  Error Code: " + e.getErrorCode());
            System.err.println("  Message:    " + e.getMessage());
            // for stack traces, refer to derby.log or uncomment this:
            //e.printStackTrace(System.err);
            e = e.getNextException();
        }
    }
	
	public static void main(String[] args) {
	
		new PlayGame();

		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		//String actionString = ((JButton)e.getSource()).getText();
		//if (actionString.equals("Next Quote") && questionAnswered) {
		if ( e.getSource() == this.next && questionAnswered) {
			getQuote();
			questionAnswered = false;
			quoteText.setBackground(Color.WHITE);
		}
		if ( e.getSource() == setPlayers) {
			promptNumPlayers();
		}
		if ( e.getSource() == setPlayerNames) {
			promptPlayerNames();
		}
		if ( e.getSource() == setPlayerKeys) {
			promptPlayerKeys(-1);
		}
		if ( e.getSource() == options) {
			promptOptions();
		}

		
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	

	class OptionButton extends JButton implements ActionListener {
	
		PlayGame game;
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	
		OptionButton(int x,int y, int width,int height,String cap, PlayGame game)
		{
			super(cap);
			setBounds(x,y,width,height);
			this.game=game;
			this.game.panel.add(this);
			addActionListener(this);
		}
		
	
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!game.questionAnswered) {
				game.questionAnswered = true;
				String selection = ((OptionButton)e.getSource()).getText();
				game.setScore(new PlayerChoice(0, selection) );
				game.next.requestFocusInWindow();
			}
			
		}
	}

	class ScoresTableModel extends AbstractTableModel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public int getRowCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getColumnCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

}


