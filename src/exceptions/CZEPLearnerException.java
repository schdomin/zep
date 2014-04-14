package exceptions;

public final class CZEPLearnerException extends CZEPException
{
	//ds constructor
	public CZEPLearnerException( String p_strErrorText )
	{
		super( p_strErrorText );
	}
	
	//ds default by inheritance
	private static final long serialVersionUID = 1L;
}
