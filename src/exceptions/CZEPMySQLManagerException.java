package exceptions;

public final class CZEPMySQLManagerException extends CZEPException
{
    //ds constructor
    public CZEPMySQLManagerException( String p_strErrorText )
    {
        super( p_strErrorText );
    }
    
    //ds default by inheritance
    private static final long serialVersionUID = 1L;
}
