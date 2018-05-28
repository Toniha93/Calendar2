import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class MonthPanel extends JPanel
{
	// Konstanten, um das Aussehen des Controls leichter anpassen zu können
	protected static final int MARGIN = 2;
	protected static final int HEADERHEIGHT = 20;
	private static final int WEEKS = 6;
	private static final int DAYSPERWEEK = 7;

	protected static final float HEADERFONTSIZE = 14.0f;
	protected static final float DAYFONTSIZE = 18.0f;

	protected static final Color COLOR_GRID = Color.LIGHT_GRAY;
	protected static final Color COLOR_WEEKDAY = Color.BLACK;
	protected static final Color COLOR_WEEKEND = Color.RED;
	protected static final Color COLOR_APPOINTMENTS = Color.BLUE;

	// Angezeigter Monat
	protected byte month;
	protected short year;
	private int daysOfMonth;

	// Besitzer
	JFrame owner;

	// Termin-Datenbank
	protected final Appointments appointments;

	// Schriftarten für die Kopfzeile und die einzelnen Tage
	private Font fntHeader;
	private Font fntDays;

	// Layout
	protected class Day
	{
		public int x;
		public int y;
	}

	protected int dayWidth;
	protected int dayHeight;
	protected Day days[];

	public MonthPanel(JFrame owner, Appointments appointments)
	{
		// Parameter speichern
		this.owner = owner;

		assert(appointments != null);
		this.appointments = appointments;

		// Bevorzugte Größe
		setPreferredSize(new Dimension(299, 295));

		// Event-Handler, der adjustLayout() aufruft sobald sich die Größe des Panels geändert hat
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e)
			{
				adjustLayout();
			}
		});

		// Event-Handler, der einen Doppelklick behandelt
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					final byte day = hitTest(e.getX(), e.getY());

					if (day > -1)
					{
						assert(DateUtil.checkDate(day, month, year));
						new DayWindow(owner, appointments, day, month, year).setVisible(true);

						// Neu zeichnen, da sich die Terminanzahl für einen Tag geändert haben kann
						repaint();
					}
				}
			}
		});

		// Day-Array erzeugen
		days = new Day[31];

		for (byte a = 0; a < days.length; a++)
			days[a] = new Day();
	}

	public void setMonth(byte month, short year)
	{
		// Parameter speichern
		this.month = month;
		this.year = year;

		adjustLayout();
	}

	protected void adjustLayout()
	{
		// Größe eines Tages berechnen
		final Dimension currentSize = getSize();

		int y = HEADERHEIGHT + MARGIN;

		assert(DAYSPERWEEK == 7);
		dayWidth = ((int)currentSize.getWidth() - 2*MARGIN-1) / DAYSPERWEEK;
		dayHeight = ((int)currentSize.getHeight() - (MARGIN+y+1)) / WEEKS;

		// Tage positionieren
		daysOfMonth = DateUtil.daysOfMonth(month, year);
		assert(daysOfMonth <= days.length);

		int x = DateUtil.firstDayOfMonth(month, year);

		for (byte a = 0; a < daysOfMonth; a++)
		{
			days[a].x = MARGIN + dayWidth * x++;
			days[a].y = y;

			if (x >= DAYSPERWEEK)
			{
				x = 0;
				y += dayHeight;
			}
		}

		// UI-Element neu zeichnen
		repaint();
	}

	public byte hitTest(int x, int y)
	{
		for (byte a = 0; a < daysOfMonth; a++)
			if ((x > days[a].x) && (y > days[a].y) && (x < days[a].x + dayWidth) && (y < days[a].y + dayHeight))
				return a;

		return -1;
	}

	protected void paintComponent(Graphics g)
	{
		// Hintergrund von Swing zeichnen lassen
		super.paintComponent(g);

		// Wenn g ein Graphics2D-Objekt ist, schalten wir Anti-Aliasing ein um eine sauberere Darstellung zu erhalten
		if (g instanceof Graphics2D)
			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Beim ersten Aufruf müssen die Font-Objekte initalisiert werden. Dazu wird die Standard-Schrift (die wir
		// erst über ein Graphics-Objekt bekommen können) verändert.
		if (fntHeader == null)
			fntHeader = g.getFont().deriveFont(Font.BOLD, HEADERFONTSIZE);

		if (fntDays == null)
			fntDays = g.getFont().deriveFont(DAYFONTSIZE);

		// Schriftart sichern, damit sie am Ende wiederhergestellt werden kann
		Font oldFont = g.getFont();

		// Kopfzeile zeichnen
		g.setFont(fntHeader);
		g.setColor(new Color(0x48, 0x48, 0x48));

		int x = MARGIN;
		int y = (int)((HEADERHEIGHT+HEADERFONTSIZE) / 2.0f) - 1;

		for (int a = 0; a < DateUtil.DAY_SHORTHANDS.length; a++)
		{
			final int width = g.getFontMetrics().stringWidth(DateUtil.DAY_SHORTHANDS[a]);

			g.drawString(DateUtil.DAY_SHORTHANDS[a], x + (dayWidth-width)/2, y);
			x += dayWidth;
		}

		// Tage zeichnen
		g.setFont(fntDays);

		int day = DateUtil.firstDayOfMonth(month, year);

		for (byte a=0; a < daysOfMonth; a++)
		{
			// Gitter
			g.setColor(COLOR_GRID);
			g.drawRect(days[a].x, days[a].y, dayWidth, dayHeight);

			// Farbe für die Zahl setzen
			g.setColor((appointments.countAppointments(a, month, year) > 0) ? COLOR_APPOINTMENTS : (day < 5) ? COLOR_WEEKDAY : COLOR_WEEKEND);

			// Zahl zeichnen
			final String tmpStr = "" + (a + 1);
			final int width = g.getFontMetrics().stringWidth(tmpStr);

			g.drawString(tmpStr, days[a].x + (dayWidth-width)/2, days[a].y + (int)((dayHeight+DAYFONTSIZE)/2.0f) - 1);

			// Nächster Wochentag
			if (++day >= DAYSPERWEEK)
				day = 0;
		}

		// Font zurücksetzen
		g.setFont(oldFont);
	}
}