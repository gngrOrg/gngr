package org.lobobrowser.request;

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
//Surpress the EXPIRES_FORMAT unused warning - attributes seemed
// important despite being private
final class CookieParsing {
  private static final Logger logger = Logger.getLogger(CookieParsing.class.getName());
  private static final DateFormat EXPIRES_FORMAT;
  private static final DateFormat EXPIRES_FORMAT_BAK1;
  private static final DateFormat EXPIRES_FORMAT_BAK2;
  static {
    // Note: Using yy in case years are given as two digits.
    // Note: Must use US locale for cookie dates.
    final Locale locale = Locale.US;
    final SimpleDateFormat ef1 = new SimpleDateFormat("EEE, dd MMM yy HH:mm:ss 'GMT'", locale);
    final SimpleDateFormat ef2 = new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss 'GMT'", locale);
    final SimpleDateFormat ef3 = new SimpleDateFormat("EEE MMM dd HH:mm:ss yy 'GMT'", locale);
    final TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT");
    ef1.setTimeZone(gmtTimeZone);
    ef2.setTimeZone(gmtTimeZone);
    ef3.setTimeZone(gmtTimeZone);
    EXPIRES_FORMAT = ef1;
    EXPIRES_FORMAT_BAK1 = ef2;
    EXPIRES_FORMAT_BAK2 = ef3;
  }

  static Optional<CookieDetails> parseCookieSpec(final URI requestURL, final String cookieSpec) {
    final StringTokenizer tok = new StringTokenizer(cookieSpec, ";");
    String cookieName = null;
    String cookieValue = null;
    String domain = null;
    String path = null;
    Optional<Date> expires = Optional.empty();
    Long maxAge = null;
    boolean secure = false;
    boolean httpOnly = false;
    boolean hasCookieName = false;
    while (tok.hasMoreTokens()) {
      final String token = tok.nextToken();
      if (!hasCookieName) {
        final Matcher matcher = COOKIE_PAIR_STRICT_MEDIUM.matcher(token);
        // if ((idx == -1) || (name.length() == 0)) {
        if (!matcher.lookingAt()) {
          return Optional.empty();
        } else {
          cookieName = matcher.group(1); // the regexp should trim() already
          // cookieValue = matcher.group(3); // [2] is quotes or empty-string
          cookieValue = matcher.group(2);
          hasCookieName = true;
        }
      } else {
        final int idx = token.indexOf('=');
        final String name = idx == -1 ? token.trim() : token.substring(0, idx).trim();
        final String value = idx == -1 ? "" : token.substring(idx + 1).trim();
        if ("max-age".equalsIgnoreCase(name)) {
          try {
            maxAge = Long.parseLong(value);
          } catch (final NumberFormatException e) {
            // Ignore this attribute
            logger.log(Level.WARNING, "parseCookieSpec(): Max-age is not formatted correctly: " + value + ".");
          }
        } else if ("path".equalsIgnoreCase(name)) {
          path = value;
        } else if ("domain".equalsIgnoreCase(name)) {
          if (value.length() == 0) {
            // Ignore this attribute
            logger.log(Level.WARNING, "parseCookieSpec(): domain is empty, hence attribute is ignored");
          } else {
            if (value.charAt(0) == '.') {
              domain = value.substring(1);
            } else {
              domain = value;
            }
          }
        } else if ("expires".equalsIgnoreCase(name)) {
          final Optional<Date> parsedExpires = parseExpiresRFC6265(value);
          if (parsedExpires.isPresent()) {
            expires = parsedExpires;
          }
        } else if ("secure".equalsIgnoreCase(name)) {
          secure = true;
        } else if ("httponly".equalsIgnoreCase(name)) {
          httpOnly = true;
        }
      }
    }
    return Optional.of(new CookieDetails(requestURL, cookieName, cookieValue, domain, path, expires, maxAge, secure, httpOnly));
  }

  private static final Map<String, Integer> monthToNum = new HashMap<>();
  static {
    monthToNum.put("jan", 1);
    monthToNum.put("feb", 2);
    monthToNum.put("mar", 3);
    monthToNum.put("apr", 4);
    monthToNum.put("may", 5);
    monthToNum.put("jun", 6);
    monthToNum.put("jul", 7);
    monthToNum.put("aug", 8);
    monthToNum.put("sep", 9);
    monthToNum.put("oct", 10);
    monthToNum.put("nov", 11);
    monthToNum.put("dec", 12);
  }

  private static final Pattern DATE_DELIM = Pattern.compile("[\\x09\\x20-\\x2F\\x3B-\\x40\\x5B-\\x60\\x7B-\\x7E]");

  //From RFC2616 S2.2:
  private static final Pattern TOKEN = Pattern.compile("[\\x21\\x23-\\x26\\x2A\\x2B\\x2D\\x2E\\x30-\\x39\\x41-\\x5A\\x5E-\\x7A\\x7C\\x7E]");

  //From RFC6265 S4.1.1
  //note that it excludes \x3B ";"
  private static final Pattern COOKIE_OCTET = Pattern.compile("[\\x21\\x23-\\x2B\\x2D-\\x3A\\x3C-\\x5B\\x5D-\\x7E]");
  // private static final Pattern COOKIE_OCTETS = Pattern.compile('^' + COOKIE_OCTET.pattern() + '$');

  //The name/key cannot be empty but the value can (S5.2):
  // private static final Pattern COOKIE_PAIR_STRICT = Pattern.compile("^(" + TOKEN.pattern() + "+)=(\"?)(" + COOKIE_OCTET.pattern() + "*)\\2$");
  private static final Pattern COOKIE_PAIR_STRICT_MEDIUM = Pattern.compile("^(" + TOKEN.pattern() + "+)=((\"?)(" + COOKIE_OCTET.pattern()
      + "*)\\3).*$");
  // private static final Pattern COOKIE_PAIR = Pattern.compile("^([^=\\s]+)\\s*=\\s*(\"?)\\s*(.*)\\s*\\2\\s*$");

  //RFC6265 S4.1.1 defines extension-av as 'any CHAR except CTLs or ";"'
  //Note ';' is \x3B
  // private static final Pattern NON_CTL_SEMICOLON = Pattern.compile("[\\x20-\\x3A\\x3C-\\x7E]+");
  // private static final Pattern EXTENSION_AV = NON_CTL_SEMICOLON;
  // private static final Pattern PATH_VALUE = NON_CTL_SEMICOLON;

  //Used for checking whether or not there is a trailing semi-colon
  // private static final Pattern TRAILING_SEMICOLON = Pattern.compile(";+$");

  /* RFC6265 S5.1.1.5:
   * [fail if] the day-of-month-value is less than 1 or greater than 31
   */
  private static final Pattern DAY_OF_MONTH = Pattern.compile("^(0?[1-9]|[12][0-9]|3[01])$");

  /* RFC6265 S5.1.1.5:
   * [fail if]
   * *  the hour-value is greater than 23,
   * *  the minute-value is greater than 59, or
   * *  the second-value is greater than 59.
   */
  // private static final Pattern TIME = Pattern.compile("(0?[0-9]|1[0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])");
  // private static final Pattern STRICT_TIME = Pattern.compile("^(0?[0-9]|1[0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])$");
  private static final Pattern LENIENT_TIME = Pattern.compile("([0-9][0-9]?):([0-9][0-9]?):([0-9][0-9]?)");

  private static final Pattern MONTH = Pattern.compile("^(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec).*$", Pattern.CASE_INSENSITIVE);
  private static final Pattern YEAR = Pattern.compile("^([0-9][0-9]{1,3})$");

  static Optional<java.util.Date> parseExpiresRFC6265(final String expiresStr) {
    final Optional<java.util.Date> expiresDate = Optional.empty();

    final String[] tokens = DATE_DELIM.split(expiresStr);

    if (tokens.length == 0) {
      return expiresDate;
    }
    boolean found_time = false, found_dom = false, found_month = false, found_year = false;

    // LocalDateTime date = LocalDateTime.MIN;
    // date = date.withNano(0);
    // LocalDate date = LocalDate.MIN;
    int dayOfMonth = 0;
    int month = 0;
    int year = 0;
    int hour = 0;
    int minute = 0;
    int second = 0;
    // time = time.withNano(0);

    for (final String token2 : tokens) {
      final String token = token2.trim();
      if (token.length() == 0) {
        continue;
      }

      // var result;

      /* 2.1. If the found-time flag is not set and the token matches the time
       * production, set the found-time flag and set the hour- value,
       * minute-value, and second-value to the numbers denoted by the digits in
       * the date-token, respectively.  Skip the remaining sub-steps and continue
       * to the next date-token.
       */
      if (!found_time) {
        // final Matcher matcher = (strict ? STRICT_TIME : LENIENT_TIME).matcher(token);
        final Matcher matcher = LENIENT_TIME.matcher(token);
        if (matcher.matches()) {
          found_time = true;
          hour = Integer.parseInt(matcher.group(1));
          minute = Integer.parseInt(matcher.group(2));
          second = Integer.parseInt(matcher.group(3));
          continue;
        }
      }

      /* 2.2. If the found-day-of-month flag is not set and the date-token matches
       * the day-of-month production, set the found-day-of- month flag and set
       * the day-of-month-value to the number denoted by the date-token.  Skip
       * the remaining sub-steps and continue to the next date-token.
       */
      if (!found_dom) {
        final Matcher matcher = DAY_OF_MONTH.matcher(token);
        if (matcher.matches()) {
          found_dom = true;
          dayOfMonth = Integer.parseInt(matcher.group(1));
          continue;
        }
      }

      /* 2.3. If the found-month flag is not set and the date-token matches the
       * month production, set the found-month flag and set the month-value to
       * the month denoted by the date-token.  Skip the remaining sub-steps and
       * continue to the next date-token.
       */
      if (!found_month) {
        final Matcher matcher = MONTH.matcher(token);
        if (matcher.matches()) {
          found_month = true;
          month = monthToNum.get(matcher.group(1).toLowerCase());
          continue;
        }
      }

      /* 2.4. If the found-year flag is not set and the date-token matches the year
       * production, set the found-year flag and set the year-value to the number
       * denoted by the date-token.  Skip the remaining sub-steps and continue to
       * the next date-token.
       */
      if (!found_year) {
        final Matcher matcher = YEAR.matcher(token);
        if (matcher.matches()) {
          year = Integer.parseInt(matcher.group(1));
          /* From S5.1.1:
           * 3.  If the year-value is greater than or equal to 70 and less
           * than or equal to 99, increment the year-value by 1900.
           * 4.  If the year-value is greater than or equal to 0 and less
           * than or equal to 69, increment the year-value by 2000.
           */
          if ((70 <= year) && (year <= 99)) {
            year += 1900;
          } else if ((0 <= year) && (year <= 69)) {
            year += 2000;
          }

          if (year < 1601) {
            return expiresDate; // 5. ... the year-value is less than 1601
          }

          found_year = true;
          continue;
        }
      }
    }

    if (!(found_time && found_dom && found_month && found_year)) {
      // 5. ... at least one of the found-day-of-month, found-month, found-year, or found-time flags is not set,
      return expiresDate;
    } else {
      try {
        final OffsetDateTime dateTime = OffsetDateTime.of(year, month, dayOfMonth, hour, minute, second, 0, ZoneOffset.UTC);
        final long millis = dateTime.toInstant().toEpochMilli();
        return Optional.of(new Date(millis));
      } catch (final DateTimeException e) {
        logger.log(Level.WARNING, "parseExpires(): Bad date-time.", e);
        return Optional.empty();
      }
    }

  }

}
