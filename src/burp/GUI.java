package burp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextArea;
import java.awt.Font;
import java.awt.FlowLayout;
import javax.swing.JSplitPane;
import java.awt.Component;
import java.awt.Desktop;

import javax.swing.Box;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JScrollPane;


import burp.BurpExtender;

public class GUI extends JFrame {
	
    private String ExtenderName = "Domain Hunter v0.1 by bit4";
    private String github = "https://github.com/bit4woo/domain_hunter";
    private Set subdomainofset = new HashSet();
    private Set domainlikeset = new HashSet();

	private JPanel contentPane;
	private JTextField textFieldSubdomains;
	private JTextField textFieldDomainsLike;
	private JLabel lblSubDomainsOf;
	private JButton btnSearch;
	private JPanel panel_2;
	private JLabel lblNewLabel_2;
	private JSplitPane splitPane;
	private Component verticalStrut;
	private JTextArea textArea;
	private JTextArea textArea_1;
	private JButton btnNewButton;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GUI frame = new GUI();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public GUI() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 930, 497);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JPanel panel = new JPanel();
		FlowLayout flowLayout_1 = (FlowLayout) panel.getLayout();
		flowLayout_1.setAlignment(FlowLayout.LEFT);
		contentPane.add(panel, BorderLayout.NORTH);
		
		lblSubDomainsOf = new JLabel("SubDomains of  ");
		panel.add(lblSubDomainsOf);
		
		textFieldSubdomains = new JTextField();
		textFieldSubdomains.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent arg0) {
				String domain = textFieldSubdomains.getText();
				textFieldDomainsLike.setText(domain.substring(0,domain.lastIndexOf(".")));
			}
		});
		panel.add(textFieldSubdomains);
		textFieldSubdomains.setColumns(20);
		
		verticalStrut = Box.createVerticalStrut(20);
		panel.add(verticalStrut);
		
		JLabel lblDomainsLike = new JLabel("Domains like ");
		panel.add(lblDomainsLike);
		
		textFieldDomainsLike = new JTextField();
		panel.add(textFieldDomainsLike);
		textFieldDomainsLike.setColumns(20);
		
		btnSearch = new JButton("search");
		btnSearch.setToolTipText("Do a single search from site map");
		btnSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String subdomain = lblSubDomainsOf.getText();
				String domainlike = lblDomainsLike.getText();
				//search(subdomain,domainlike);
				//textArea.setText(subdomainofset);
				//textArea_1.setText(domainlikeset);
			}
		});
		panel.add(btnSearch);
		
		btnNewButton = new JButton("Spider all & Search");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String subdomain = lblSubDomainsOf.getText();
				String domainlike = lblDomainsLike.getText();
				//spiderall(subdomain,domainlike);
				//textArea.setText(subdomainofset);
				//textArea_1.setText(domainlikeset);
			}
		});
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
		    
			}
		});
		btnNewButton.setToolTipText("this may take 10min! spider and search recursively.");
		panel.add(btnNewButton);
		
		splitPane = new JSplitPane();
		splitPane.setDividerLocation(0.5);
		contentPane.add(splitPane, BorderLayout.CENTER);
		
		textArea = new JTextArea();
		textArea.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				final JPopupMenu jp = new JPopupMenu();
		        jp.add("^_^");
		        textArea.addMouseListener(new MouseAdapter() {
		            @Override
		            public void mouseClicked(MouseEvent e) {
		                if (e.getButton() == MouseEvent.BUTTON3) {
		                    // µ¯³ö²Ëµ¥
		                    jp.show(textArea, e.getX(), e.getY());
		                }
		            }
		        });
			}
		});
		textArea.setColumns(30);
		splitPane.setLeftComponent(textArea);
		
		textArea_1 = new JTextArea();
		textArea_1.setColumns(30);
		splitPane.setRightComponent(textArea_1);
		
		panel_2 = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel_2.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		contentPane.add(panel_2, BorderLayout.SOUTH);
		
		lblNewLabel_2 = new JLabel("    "+github);
		lblNewLabel_2.setFont(new Font("ËÎÌå", Font.BOLD, 12));
		lblNewLabel_2.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					URI uri = new URI(github);
					Desktop desktop = Desktop.getDesktop();
					if(Desktop.isDesktopSupported()&&desktop.isSupported(Desktop.Action.BROWSE)){
						desktop.browse(uri);
					}
				} catch (Exception e2) {
					// TODO: handle exception
					//callbacks.printError(e2.getMessage());
				}
				
			}
			@Override
			public void mouseEntered(MouseEvent e) {
				lblNewLabel_2.setForeground(Color.BLUE);
			}
			@Override
			public void mouseExited(MouseEvent e) {
				lblNewLabel_2.setForeground(Color.BLACK);
			}
		});
		panel_2.add(lblNewLabel_2);
	}

}
