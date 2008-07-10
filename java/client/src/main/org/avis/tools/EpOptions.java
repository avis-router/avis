package org.avis.tools;

/**
 * The ep command line options.
 * 
 * @author Matthew Phillips
 */
public class EpOptions extends ToolOptions
{
  public static final String DESCRIPTION = 
    "Inject notifications into an Elvin router";
  
  public static final String USAGE =   
    COMMON_USAGE_SUMMARY + "\n\n" + 
    COMMON_USAGE_DETAIL + 
    "\n" +
    "  Example message:\n\n" +
    "    string: \"a string\"\n" +
    "    number: 42\n" +
    "    long:   17L\n" +
    "    real:   3.1415\n" +
    "    opaque: [de ad be ef f0 0d]\n" +
    "    ---\n\n" +
    "  Use \\ to quote specical characters";
  
  public EpOptions (String... args)
  {
    super (args);
  }
}