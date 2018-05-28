import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class DayWindow extends JDialog
{
	// Angezeigter Tag
	private final byte day;
	private final byte month;
	private final short year;

	// Termin-Datenbank	und Liste aller Termine an diesem Tag
	private final Appointments appointments;
	private java.util.List<Appointment> appointmentsList;

	// UI-Elemente
	private JLabel lblDate;
	private JTable tblAppointments;
	private JButton btnAdd;
	private JButton btnRemove;

	public DayWindow(JFrame owner, Appointments appointments, byte day, byte month, short year)
	{
		super(owner, "Tagesansicht", true);

		// Parameter speichern
		assert(appointments != null);

		this.appointments = appointments;
		this.day = day;
		this.month = month;
		this.year = year;

		// Kontrollelemente erzeugen und einf�gen
		add(createDatePanel(), BorderLayout.NORTH);
		add(createAppointmentsTable(), BorderLayout.CENTER);
		add(createButtonsPanel(), BorderLayout.SOUTH);

		// Kontrollelemente aktualisieren
		updateViews();

		// Position und Gr��e aller Kontrollelemente neu berechnen
		pack();

		// Das Fenster soll sich in der Mitte des Elternfensters �ffnen
		setLocationRelativeTo(owner);
	}

	protected void updateViews()
	{
		// Termine f�r den aktuellen Tag aufbereiten
		(appointmentsList=appointments.getAppointments(day, month, year)).sort(new Comparator<Appointment>()
		{
			public int compare(Appointment o1, Appointment o2)
			{
				// Durch das Speichern der Startzeit als Anzahl der Minuten nach Mitternacht ist es
				// besonders einfach zwei Startzeiten f�r das Sortieren der Termine zu vergleichen:
				return o1.getStart() - o2.getStart();
			}
		});

		// Tabelle neu zeichnen lassen
		((AbstractTableModel)tblAppointments.getModel()).fireTableDataChanged();
	}

	// Datumsanzeige erzeugen
	private JComponent createDatePanel()
	{
		JPanel panel = new JPanel(new GridBagLayout());

		lblDate = new JLabel();
		lblDate.setFont(lblDate.getFont().deriveFont(24.0f));
		lblDate.setText(String.format("%d. %s %d", day + 1, DateUtil.MONTHS[month], year));

		// Das GridBagLayout bietet uns die M�glichkeit, das Label mit gr��erem Rand (8px) zu platzieren.
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(8, 8, 8, 8);

		panel.add(lblDate, c);

		return panel;
	}

	private JComponent createButtonsPanel()
	{
		JPanel panel  = new JPanel();

		// Button zum Hinzuf�gen eines neuen Termins
		(btnAdd=new JButton("Hinzuf�gen...")).addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				editAppointment(null);
			}
		});

		panel.add(btnAdd);

		// Button zum Entfernen eines neuen Termins
		(btnRemove=new JButton("L�schen")).addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				// Durch diese Schleife werden ggf. mehrere ausgew�hlte Termine entfernt.
				for (int index : tblAppointments.getSelectedRows())
					appointments.remove(appointmentsList.get(index));

				updateViews();
			}
		});

		// Der Button ist zun�chst deaktiviert. Er wird aktiv, sobald mindestens ein Termin
		// in der Liste ausgew�hlt wird (siehe ListSelectionListener).
		btnRemove.setEnabled(false);

		panel.add(btnRemove);

		return panel;
	}

	// Appointment editieren
	protected void editAppointment(Appointment appointment)
	{
		Appointment appointmentCopy = new Appointment(appointment);

		// Beim Hinzuf�gen eines Appointments setzen wir das Datum ein
		if (appointment == null)
			appointmentCopy.setDate(day, month, year);

		EditAppointmentPanel panel = new EditAppointmentPanel(appointmentCopy);
		if (JOptionPane.showConfirmDialog(DayWindow.this, panel, "Termin bearbeiten", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION)
		{
			try
			{
				// Aktualisiert das Appointment-Objekt mit den Eingaben des Benutzers.
				// Im Fehlerfall l�st check() eine IllegalArgumentExcepion oder eine IllegalStateExcepion mit Nachricht aus.
				panel.updateAppointment();

				// Das urspr�nglich Appointment l�schen
				if (appointment != null)
					appointments.remove(appointment);

				// Das ver�nderte Appointment hinzuf�gen
				appointments.add(appointmentCopy);

				updateViews();
			}
			catch (IllegalArgumentException | IllegalStateException e)
			{
				JOptionPane.showMessageDialog(DayWindow.this, e.getMessage(), "Ung�ltiger Termin", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	// Appointments-Tabelle erzeugen
	private JComponent createAppointmentsTable()
	{
		(tblAppointments=new JTable(new DayViewTableModel())).addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent event)
			{
				// Doppelkltk!
				if (event.getClickCount() == 2)
				{
					final int index = tblAppointments.getSelectedRow();
					if (index != -1)
						editAppointment(appointmentsList.get(index));
				}
			}
		});

		// Der "L�schen"-Button soll nur aktiv sein, wenn auch wirklich ein Termin ausgew�hlt ist.
		tblAppointments.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				btnRemove.setEnabled(tblAppointments.getSelectedRow() != -1);
			}
		});

		// Relative Spaltenbreiten setzen, damit die ersten Spalten schmaler sind
		final TableColumnModel tableColumnModel = tblAppointments.getColumnModel();
		tableColumnModel.getColumn(0).setPreferredWidth(40);
		tableColumnModel.getColumn(1).setPreferredWidth(50);
		tableColumnModel.getColumn(2).setPreferredWidth(360);

		// Die erste und zweite Spalte sollen rechtsb�ndig formatiert werden.
		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(JLabel.RIGHT);

		tableColumnModel.getColumn(0).setCellRenderer(rightRenderer);
		tableColumnModel.getColumn(1).setCellRenderer(rightRenderer);

		// Die JTable wird in eine JScrollPane verpackt, damit wir scrollen k�nnen.
		JScrollPane scrollPane = new JScrollPane(tblAppointments);
		scrollPane.setPreferredSize(new Dimension(450, 220));

		return scrollPane;
	}

	private class DayViewTableModel extends AbstractTableModel
	{
		public int getColumnCount()
		{
			return 3;
		}

		public String getColumnName(int column)
		{
			switch (column)
			{
			case 0:
				return "#";

			case 1:
				return "Beginn";

			case 2:
				return "Betreff";
			}

			return "?";
		}

		public int getRowCount()
		{
			return appointmentsList.size();
		}

		public Object getValueAt(int row, int column)
		{
			assert((column >= 0 )&& (column <= 2));
			assert(row < appointmentsList.size());

			if (column == 0)
				return row + 1;

			Appointment app = appointmentsList.get(row);

			return (column == 1 ) ? String.format("%2d:%02d", app.getStart() / 60, app.getStart() % 60) : app.getSubject();
		}
	}
}