package exceptions;

public final class CZEPConfigurationException extends CZEPException
{
	//ds constructor
	public CZEPConfigurationException( String p_strErrorText )
	{
		super( p_strErrorText );
	}
	
	//ds default by inheritance
	private static final long serialVersionUID = 1L;
}
