package org.httpobjects;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This should be re-implemented acording to the spec's exact algorithm as specified in https://tools.ietf.org/html/rfc6265#section-5.1.1
 *
 */
public class DateTimeRFC6265 {
    public enum DayOfWeek {
        MONDAY("Mon"),
        TUESDAY("Tue"),
        WEDNESDAY("Wed"),
        THURSDAY("Thu"),
        FRIDAY("Fri"),
        SATURDAY("Sat"),
        SUNDAY("Sun");

        public static DayOfWeek getForAbbreviation(String abbreviation){
            for(DayOfWeek next : DayOfWeek.values()){
                if(next.abbreviation.toLowerCase().equals(abbreviation.toLowerCase())) return next;
            }
            return null;
        }

        public static DateTimeRFC6265.DayOfWeek getForDayNumStartingSunday(int i){
            switch (i){
                case 1 : return DateTimeRFC6265.DayOfWeek.SUNDAY;
                case 2 : return  DateTimeRFC6265.DayOfWeek.MONDAY;
                case 3 : return  DateTimeRFC6265.DayOfWeek.TUESDAY;
                case 4 : return  DateTimeRFC6265.DayOfWeek.WEDNESDAY;
                case 5 : return  DateTimeRFC6265.DayOfWeek.THURSDAY;
                case 6 : return  DateTimeRFC6265.DayOfWeek.FRIDAY;
                case 7 : return  DateTimeRFC6265.DayOfWeek.SATURDAY;
                default: throw new RuntimeException("bad week day: " + i);
            }
        }

        private final String abbreviation;
        DayOfWeek(String abbreviation){
            this.abbreviation = abbreviation;
        }
    }
    public enum MonthOfYear {
        JANUARY("Jan"),
        FEBRUARY("Feb"),
        MARCH("Mar"),
        APRIL("Apr"),
        MAY("May"),
        JUNE("Jun"),
        JULY("Jul"),
        AUGUST("Aug"),
        SEPTEMBER("Sep"),
        OCTOBER("Oct"),
        NOVEMBER("Nov"),
        DECEMBER("Dec");

        static MonthOfYear getForAbbreviation(String abbreviation){
            for(MonthOfYear next : MonthOfYear.values()){
                if(next.abbreviation.toLowerCase().equals(abbreviation.toLowerCase())) return next;
            }
            return null;
        }

        private final String abbreviation;
        MonthOfYear(String abbreviation){
            this.abbreviation = abbreviation;
        }
    }

    private static final Pattern pattern = Pattern.compile("([A-Za-z]*), ([0-9]*)[ -]([A-Za-z]*)[ -]([0-9]*) ([0-9]*):([0-9]*):([0-9]*).* ([A-Za-z]*)");

    final String text;
    final DayOfWeek dayOfWeek;
    final int dayOfMonth;
    final MonthOfYear monthOfYear;
    final int year;
    final int hours;
    final int minutes;
    final int seconds;
    final String timezone;

    public DateTimeRFC6265(DayOfWeek dayOfWeek, int dayOfMonth, MonthOfYear monthOfYear, int year, int hours, int minutes, int seconds, String timezone){
        this.dayOfWeek = dayOfWeek;
        this.dayOfMonth = dayOfMonth;
        this.monthOfYear = monthOfYear;
        this.year = year;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.timezone = timezone;
        this.text = String.format("%s, %02d %s %02d %02d:%02d:%02d %s", dayOfWeek.abbreviation, dayOfMonth, monthOfYear.abbreviation, year, hours, minutes, seconds, timezone);
    }

    public DateTimeRFC6265(String text){
        this.text = text;

        final Matcher match = pattern.matcher(text);
        if(match.matches()) {// side-effect: call to matches() )actually starts the matching

            dayOfWeek = DayOfWeek.getForAbbreviation(match.group(1));
            dayOfMonth = Integer.parseInt(match.group(2));
            monthOfYear = MonthOfYear.getForAbbreviation(match.group(3));
            final int yyyy = Integer.parseInt(match.group(4));
            year = (yyyy <= 69) ? yyyy + 2000 : (yyyy <= 99) ? yyyy + 1900 : yyyy;
            hours = Integer.parseInt(match.group(5));
            minutes = Integer.parseInt(match.group(6));
            seconds = Integer.parseInt(match.group(7));
            timezone = match.group(8);
        }else{
            throw new RuntimeException("Not a date I understand: '" + text + "'");
        }
    }

    @Override
    public String toString(){
        return text;
    }

    @Override
    public int hashCode(){
        return text.hashCode();
    }

    @Override
    public boolean equals(Object o){
        return o instanceof DateTimeRFC6265 && o.toString().equals(toString());
    }

    public static DateTimeRFC6265 fromCalendar(java.util.Calendar c) {
            return new DateTimeRFC6265(
                DateTimeRFC6265.DayOfWeek.getForDayNumStartingSunday(c.get(Calendar.DAY_OF_WEEK)),
                c.get(Calendar.DAY_OF_MONTH), //dayOfMonth
                DateTimeRFC6265.MonthOfYear.values()[c.get(Calendar.MONTH)], // monthOfYear,
                c.get(Calendar.YEAR), // int year,
                c.get(Calendar.HOUR_OF_DAY), // int hours,
                c.get(Calendar.MINUTE), // int minutes,
                c.get(Calendar.SECOND), // int seconds,
                c.getTimeZone().getID() // String timezone
        );
    }
}
