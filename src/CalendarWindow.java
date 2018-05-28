import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

public class CalendarWindow extends JFrame
{
	// Angezeigter Monat
	protected byte month;
	protected short year;

	// Termin-Datenbank
	protected final Appointments appointments;

	// UI-Elemente
	private JButton btnPreviousMonth;
	private JButton btnNextMonth;
	private JLabel lblCurrentMonth;
	private MonthPanel pnlAppointments;

	public CalendarWindow(Appointments appointments, byte month, short year)
	{
		// Titel
		super("Java-Kalender");

		// Parameter speichern
		assert(appointments != null);

		this.appointments = appointments;
		this.month = month;
		this.year = year;

		// Standard-Operation zum Schlie�en des Fensters
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Kontrollelemente erzeugen und einf�gen
		add(createControls(), BorderLayout.NORTH);
		add((pnlAppointments=new MonthPanel(this, appointments)), BorderLayout.CENTER);

		// Kontrollelemente aktualisieren
		updateViews();

		// Position und Gr��e aller Kontrollelemente neu berechnen
		pack();

		// Das Fenster soll sich in der Mitte des Bildschirms �ffnen
		setLocationRelativeTo(null);
	}

	protected void updateViews()
	{
		// Monat setzen
		pnlAppointments.setMonth(month, year);

		// Buttons
		btnPreviousMonth.setEnabled((year>1900) || (month>0));
		btnNextMonth.setEnabled((year<2099) || (month<11));

		// Label-Text
		lblCurrentMonth.setText(DateUtil.MONTHS[month] + " " + year);
	}

	// Kontrollelemente am oberen Fensterrand erzeugen
	private JComponent createControls()
	{
		JPanel panel = new JPanel(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5, 5, 5, 5);

		// Button zum vorherigen Monat
		(btnPreviousMonth=new JButton("<")).addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (--month < 0)
				{
					month = 11;
					year--;
				}

				updateViews();
			}
		});

		c.gridx = c.gridy = 0;
		panel.add(btnPreviousMonth, c);

		// Label mit dem aktuellen Monat/Jahr
		lblCurrentMonth = new JLabel();

		c.gridx = 1;
		panel.add(lblCurrentMonth, c);

		// Button zum n�chsten Monat
		(btnNextMonth=new JButton(">")).addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (++month > 11)
				{
					month = 0;
					year++;
				}

				updateViews();
			}
		});

		c.gridx = 2;
		panel.add(btnNextMonth,c);

		// Button f�r den Auswahldialog
		JButton btnSelectMonth = new JButton("Anderer Monat...");
		btnSelectMonth.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				SelectMonthPanel panel = new SelectMonthPanel(month, year);

				if (JOptionPane.showConfirmDialog(CalendarWindow.this, panel, "Anderer Monat", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION)
				{
					try
					{
						// Pr�fen, ob der Benutzer ein g�ltiges Datum eingegeben hat.
						// Im Fehlerfall l�st check() eine IllegalStateExcepion mit Nachricht aus.
						panel.check();

						// Monat und Jahr aus dem Dialogfenster �bernehmen
						month = panel.getMonth();
						year =  panel.getYear();

						updateViews();
					}
					catch (IllegalStateException e)
					{
						JOptionPane.showMessageDialog(CalendarWindow.this, e.getMessage(), "Ung�ltiges Datum", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		c.gridx = c.gridy = 1;
		panel.add(btnSelectMonth, c);

		// Fertiges Panel zur�ckliefern
		return panel;
	}
}