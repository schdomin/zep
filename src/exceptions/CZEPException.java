package exceptions;

public abstract class CZEPException extends Exception
{
	//ds constructor
	CZEPException( String p_strErrorText )
	{
		//ds set the error text
		super( p_strErrorText );
	}
	
	//ds default by inheritance
	private static final long serialVersionUID = 1L;
}
