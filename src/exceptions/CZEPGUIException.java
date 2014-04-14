package exceptions;

public final class CZEPGUIException extends CZEPException
{
    //ds constructor
    public CZEPGUIException( String p_strErrorText )
    {
        super( p_strErrorText );
    }
    
    //ds default by inheritance
    private static final long serialVersionUID = 1L;
}
