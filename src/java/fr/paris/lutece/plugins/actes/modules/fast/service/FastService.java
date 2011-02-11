/*
 * Copyright (c) 2002-2009, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.actes.modules.fast.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import fr.gouv.interieur.actes_v1.Annulation;
import fr.gouv.interieur.actes_v1.DonneesActe;
import fr.gouv.interieur.actes_v1.FichierSigne;
import fr.gouv.interieur.actes_v1.DonneesActe.Annexes;
import fr.paris.lutece.plugins.actes.util.ActeNamespacePrefixMapper;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.date.DateUtil;


/**
 * S2LOW implementation
 */
public class FastService
{
    private static final Locale LOCALE = new Locale( "fr" );
    private static final String EXTENSION_PDF_PJ = ".pdf";
    private static final String EXTENSION_XML = ".xml";
    private static final String EXTENSION_WS = ".ws";
    private static final String EXTENSION_OK = ".OK";
    private static final String PATH_SEPARATOR = "/";
    private static final String NAME_SEPARATOR = "-";
    private static final String NAME_UNDERSCOR_SEPARATOR = "_";
    private static final String ETC = "...";
    private static final String NUMERO_ZERO_PJ = "0";
    private static final String PROPERTY_ACTE_DATE_CLASSIFICATION = "actes-fast.date.classification";
    private static final String PROPERTY_ACTE_SIREN_VILLE = "actes-fast.odsville.siren";
    private static final String PROPERTY_ACTE_SIREN_DEPT = "actes-fast.odsdept.siren";
    private static final String PROPERTY_ACTE_DIR_PATH = "actes-fast.tmp.path.directory";
    private static final String PROPERTY_ACTE_FAST_DIR_PATH = "actes-fast.fast.path.directory";
    private static final String PROPERTY_ACTE_DEPT = "actes-fast.departement";
    private static final String PROPERTY_ACTE_TRANSACTION_TRANSMISSION = "actes-fast.transaction.transmission";
    private static final String PROPERTY_ACTE_TRANSACTION_ANNULATION = "actes-fast.transaction.annulation";
    private static final String PROPERTY_ACTE_WS_TRAITEMENT = "actes-fast.ws.traitement";
    private static final String PROPERTY_ACTE_WS_DNUTILISATEUR_VILLE = "actes-fast.ws.odsville.dnutilisateur";
    private static final String PROPERTY_ACTE_WS_DNUTILISATEUR_DEPT = "actes-fast.ws.odsdept.dnutilisateur";
    private static final String PROPERTY_ACTE_TYPE_LIBELLE = "actes-fast.type.libelle";
    private static final String PROPERTY_ACTE_TYPE_NUM = "actes-fast.type.num";
    private static final String PROPERTY_NB_CARACTERES_MAX = "actes-fast.nb.caracteres.max";
    private static final String PROPERTY_CARACTERES_DEFICIENT = "actes-fast.caracteres.deficient";
    private static final int NB_CARACTERES_MAX_DEFAULT = 400;
    private static final String XML_ENTETE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String XML_WS_ENTETE = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:cascl=\"http://dev.cdcfast.fr/connecteur/V20\"><SOAP-ENV:Body>";
    private static final String XML_WS_FERMETURE_ENTETE = "</SOAP-ENV:Body></SOAP-ENV:Envelope>";
    private static final String XML_WS_BALISE_ACTE = "<cascl:traiterACTES>";
    private static final String XML_WS_BALISE_FERMETURE_ACTE = "</cascl:traiterACTES>";
    private static final String XML_WS_BALISE_TRAITEMENT = "<cascl:typeTraitement>";
    private static final String XML_WS_BALISE_FERMETURE_TRAITEMENT = "</cascl:typeTraitement>";
    private static final String XML_WS_BALISE_DNUTILISATEUR = "<cascl:DNUtilisateur>";
    private static final String XML_WS_BALISE_FERMETURE_DNUTILISATEUR = "</cascl:DNUtilisateur>";
    private static final String XML_WS_BALISE_SIREN = "<cascl:SIREN>";
    private static final String XML_WS_BALISE_FERMETURE_SIREN = "</cascl:SIREN>";
    private static final String XML_WS_BALISE_FICHIER = "<cascl:fichierACTES>";
    private static final String XML_WS_BALISE_FERMETURE_FICHIER = "</cascl:fichierACTES>";
    private static final String XML_ENCODING = "actes-fast.xml.encoding";
    private static final String XML_ENCODING_DEFAULT = "ISO-8859-1";
    private static final String CONSTANTE_CARACTERS_DEFICIENT_SEPARATOR = ",";
    private static final String CONSTANTE_CARACTERS_DEFICIENT_REPLACE = "?";
    private static final String EMPTY_STRING = "";
    private static final String CONSTANTE_PATTERN_DATE = "yyyyMMdd";

    /**
     * Constructeur vide
     */
    public FastService(  )
    {
    }

    /**
     * Envoi une transmission d'un acte
     * @param strNumeroDeliberation le numero de la déliberation
     * @param listFile la liste des fichiers rattachés à l'acte
     * @param deliberationFinal la délibération finale
     * @param strObjet l'objet de l'acte
     * @param nCodeMatiere1 le codeMatiere 1 de l'acte
     * @param nCodeMatiere2 le codeMatiere 2 de l'acte
     * @param bIsMunicipal true si la formation conseil est MUNICIPAL
     * @param tsDateDecision la date de vote/decision de l'acte
     * @return true si la transmission s'est bien déroulée, false sinon
     * @throws IOException IOException
     */
    public boolean sendActe( String strNumeroDeliberation, List<byte[]> listFile, byte[] deliberationFinal,
        String strObjet, int nCodeMatiere1, int nCodeMatiere2, boolean bIsMunicipal, Timestamp tsDateDecision )
        throws IOException
    {
        fr.paris.lutece.plugins.actes.business.Acte acte = new fr.paris.lutece.plugins.actes.business.Acte(  );

        acte.setCodeNatureActe( Integer.parseInt( AppPropertiesService.getProperty( PROPERTY_ACTE_TYPE_NUM ) ) );
        
        String strXmlEncoding = AppPropertiesService.getProperty( XML_ENCODING, XML_ENCODING_DEFAULT);
        
        String strObjetEncoded = new String( strObjet.getBytes(), strXmlEncoding );
        
        String[] strDeficientCaracters = getDeficientCaracteres(  );
        
        for( String strCaracters : strDeficientCaracters )
        {
        	if( !strCaracters.trim(  ).equals( EMPTY_STRING ) )
        	{
        		strObjetEncoded = strObjetEncoded.replace( strCaracters.trim(  ), CONSTANTE_CARACTERS_DEFICIENT_REPLACE );
        	}
        }
        
        String strNumeroDeliberationEncoded = new String( strNumeroDeliberation.getBytes(), strXmlEncoding );
        
        //Vérification du nombre de caractères maximum autorisés
        int nNbCaracteresMax = AppPropertiesService.getPropertyInt( PROPERTY_NB_CARACTERES_MAX, NB_CARACTERES_MAX_DEFAULT );
        if ( strObjetEncoded.length() > nNbCaracteresMax )
        {
        	strObjetEncoded = strObjetEncoded.substring( 0, nNbCaracteresMax - 3 ) + ETC;
        }
        
        acte.setObjet( strObjetEncoded );
        acte.setNumeroInterne( strNumeroDeliberationEncoded );

        String strDateClassification = AppPropertiesService.getProperty( PROPERTY_ACTE_DATE_CLASSIFICATION );
        Date dateClassification = DateUtil.formatDateSql( strDateClassification, LOCALE );
        Calendar calClassificationd = new GregorianCalendar(  );
        calClassificationd.setTime( dateClassification );
        acte.setClassificationDateVersion( calClassificationd );

        //Date acte = date de vote/decision de l'acte
        GregorianCalendar calDecision = new GregorianCalendar(  );
        calDecision.setTime( tsDateDecision );
        acte.setDate( calDecision );

        //Classification acte
        DonneesActe.CodeMatiere1 cm1 = new DonneesActe.CodeMatiere1(  );
        cm1.setCodeMatiere( new Integer( nCodeMatiere1 ) );
        acte.setCodeMatiere1( cm1 );

        DonneesActe.CodeMatiere2 cm2 = new DonneesActe.CodeMatiere2(  );
        cm2.setCodeMatiere( new Integer( nCodeMatiere2 ) );
        acte.setCodeMatiere2( cm2 );

        //Construction du répertoire temporaire
        Long lCurrentDate = System.currentTimeMillis(  );
        String strTmpPath = AppPropertiesService.getProperty( PROPERTY_ACTE_DIR_PATH ) + PATH_SEPARATOR + lCurrentDate;
        boolean bIsDirectoryCreated = new File( strTmpPath ).mkdir(  );

        if ( bIsDirectoryCreated )
        {
            Annexes annexes = new Annexes(  );

            /****************************Construction des noms de fichier**********************************/
            String strTypeActe = AppPropertiesService.getProperty( PROPERTY_ACTE_TYPE_LIBELLE );
            String strDept = AppPropertiesService.getProperty( PROPERTY_ACTE_DEPT );
            String strTransaction = AppPropertiesService.getProperty( PROPERTY_ACTE_TRANSACTION_TRANSMISSION );
            String strSiren;

            if ( bIsMunicipal )
            {
                strSiren = AppPropertiesService.getProperty( PROPERTY_ACTE_SIREN_VILLE );
            }
            else
            {
                strSiren = AppPropertiesService.getProperty( PROPERTY_ACTE_SIREN_DEPT );
            }

            String strDateCurrentFormat = getDateDecision( tsDateDecision );

            //Piece jointe Annexe sans extension 
            String strNameFichierPJ = strDept + NAME_SEPARATOR + strSiren + NAME_SEPARATOR + strDateCurrentFormat +
                NAME_SEPARATOR + strNumeroDeliberation + NAME_SEPARATOR + strTypeActe + NAME_SEPARATOR + strTransaction +
                NAME_UNDERSCOR_SEPARATOR;

            //Piece jointe Metier sans extension
            String strNameFichierPJMetier = strNameFichierPJ + NUMERO_ZERO_PJ;

            //Piece jointe Délibération final sans extension
            int numeroPJ = 1;
            String strNameFichierDeliberation = strNameFichierPJ + String.valueOf( numeroPJ );

            //Création du fichier de délibération final
            creationFichierDeliberationFinale( strTmpPath, strNameFichierDeliberation, deliberationFinal );

            FichierSigne fichier = new FichierSigne(  );
            fichier.setNomFichier( strNameFichierDeliberation + EXTENSION_PDF_PJ );
            acte.setDocument( fichier );

            //Construction des fichiers piece jointe annexes	
            numeroPJ++;

            for ( byte[] b : listFile )
            {
                FileOutputStream fileOutputStream = new FileOutputStream( new File( strTmpPath + PATH_SEPARATOR +
                            strNameFichierPJ + numeroPJ + EXTENSION_PDF_PJ ) );
                BufferedOutputStream buffer = new BufferedOutputStream( fileOutputStream );
                buffer.write( b );
                buffer.close(  );

                FichierSigne fichierAnnexe = new FichierSigne(  );
                fichierAnnexe.setNomFichier( strNameFichierPJ + numeroPJ + EXTENSION_PDF_PJ );
                annexes.getAnnexe(  ).add( fichierAnnexe );
                numeroPJ++;
            }

            annexes.setNombre( numeroPJ - 2 );
            acte.setAnnexes( annexes );
            //Creation du fichier Metier
            creationFichierMetier( strTmpPath, strNameFichierPJMetier + EXTENSION_XML, acte.getXML(  ) );
            //Creation du fichier WebService
            creationFichierWebService( strTmpPath, strNameFichierPJMetier, strNameFichierPJMetier + EXTENSION_XML,
            		bIsMunicipal );
            //Copie du répertoire temporaire vers répertoire fast
            copierFichierDansFast( strTmpPath, strNameFichierPJ + NUMERO_ZERO_PJ );
            //Suppression répertoire temporaire
            supprimerRepertoireTmp( strTmpPath );
        }
        else
        {
            AppLogService.error( "Error when copying files : directory doesn't exist." );

            return false;
        }

        return true;
    }

    /**
     * Envoie une demande d'annulation d'acte
     * @param strNumeroDeliberation le numéro de la déliberation
     * @param bIsMunicipal true si la formation conseil est MUNICIPAL
     * @param tsDateDecision la date de vote/decision de l'acte
     * @return true si la transmission s'est bien déroulée, false sinon
     * @throws IOException IOException
     */
    public boolean sendAnnulationActe( String strNumeroDeliberation, boolean bIsMunicipal, Timestamp tsDateDecision )
        throws IOException
    {
        Annulation d = new Annulation(  );

        //Construction du répertoire temporaire
        Long lCurrentDate = System.currentTimeMillis(  );
        String strTmpPath = AppPropertiesService.getProperty( PROPERTY_ACTE_DIR_PATH ) + PATH_SEPARATOR + lCurrentDate;
        boolean bIsDirectoryCreated = new File( strTmpPath ).mkdir(  );

        if ( bIsDirectoryCreated )
        {
            String strTypeActe = AppPropertiesService.getProperty( PROPERTY_ACTE_TYPE_LIBELLE );
            String strDept = AppPropertiesService.getProperty( PROPERTY_ACTE_DEPT );
            String strTransaction = AppPropertiesService.getProperty( PROPERTY_ACTE_TRANSACTION_ANNULATION );
            String strSiren;

            if ( bIsMunicipal )
            {
                strSiren = AppPropertiesService.getProperty( PROPERTY_ACTE_SIREN_VILLE );
            }
            else
            {
                strSiren = AppPropertiesService.getProperty( PROPERTY_ACTE_SIREN_DEPT );
            }

            String strDateCurrentFormat = getDateDecision( tsDateDecision );
            
            /* Creation de l'id de l'acte
             * IdActe est de la forme : 075-217500055-20090707-ODS000000000074-DE 
             * Departement-Num_Siren-DateJour-ODS+NumeroDelib-Type_Acte 
             */
            String strIdActe = strDept + NAME_SEPARATOR + strSiren + NAME_SEPARATOR + strDateCurrentFormat +
            	NAME_SEPARATOR + strNumeroDeliberation + NAME_SEPARATOR + strTypeActe;
            
            d.setIDActe( strIdActe );

            //Génération du contenu XML
            String strXmlEncoding = AppPropertiesService.getProperty( XML_ENCODING, XML_ENCODING_DEFAULT); 
            StringWriter sw = new StringWriter(  );
            try
            {
                JAXBContext jaxbContext = JAXBContext.newInstance( "fr.gouv.interieur.actes_v1" );
                Marshaller marshaller = jaxbContext.createMarshaller(  );
                marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
                marshaller.setProperty( "com.sun.xml.bind.namespacePrefixMapper", new ActeNamespacePrefixMapper(  ) );
                marshaller.setProperty( Marshaller.JAXB_ENCODING, strXmlEncoding );
                marshaller.marshal( d, sw );
            }
            catch ( JAXBException e )
            {
                AppLogService.error( "Error marshalling Actes document : " + e.getMessage(  ), e );
            }

            //Piece jointe Annexe sans extension 
            String strNameFichierPJ =  strIdActe + NAME_SEPARATOR + strTransaction + NAME_UNDERSCOR_SEPARATOR;

            //Piece jointe Metier sans extension
            String strNameFichierPJMetier = strNameFichierPJ + NUMERO_ZERO_PJ;

            //Creation du fichier Metier
            creationFichierMetier( strTmpPath, strNameFichierPJMetier + EXTENSION_XML, sw.toString(  ) );
            //Creation du fichier WebService
            creationFichierWebService( strTmpPath, strNameFichierPJMetier, strNameFichierPJMetier + EXTENSION_XML,
            		bIsMunicipal );
            //Copie du répertoire temporaire vers répertoire fast
            copierFichierDansFast( strTmpPath, strNameFichierPJ + NUMERO_ZERO_PJ );
            //Suppression répertoire temporaire
            supprimerRepertoireTmp( strTmpPath );
        }
        else
        {
            return false;
        }

        return true;
    }

    /**
     * Copier les fichiers necessaires à la transmission dans le dossier local correspondant
     * @param strPath le chemin vers le dossier local de destination
     * @param strName le nom du répertoire
     * @throws IOException IOException
     */
    private void copierFichierDansFast( String strPath, String strName )
        throws IOException
    {
        String strPathFAST = AppPropertiesService.getProperty( PROPERTY_ACTE_FAST_DIR_PATH );
        File repdest = new File( strPathFAST + PATH_SEPARATOR + strName );
        repdest.mkdir(  );

        File tmpDir = new File( strPath );
        String[] fileNames = tmpDir.list(  );

        if ( fileNames != null )
        {
            //Prendre tous les fichiers du répertoire courant
            for ( int i = 0; i < fileNames.length; i++ )
            {
                FileOutputStream fileOutputStream = new FileOutputStream( new File( strPathFAST + PATH_SEPARATOR +
                            strName + PATH_SEPARATOR + fileNames[i] ) );
                byte[] buf = new byte[1024];
                int len;
                FileInputStream fin = new FileInputStream( new File( tmpDir, fileNames[i] ) );
                BufferedInputStream in = new BufferedInputStream( fin );

                while ( ( len = in.read( buf ) ) != -1 )
                {
                    fileOutputStream.write( buf, 0, len );
                }

                in.close(  );
                fileOutputStream.close(  );
            }
        }
        else
        {
            new IOException(  );
        }

        //Creation du fichier temoin
        FileWriter temoinSortie = new FileWriter( strPathFAST + PATH_SEPARATOR + strName + EXTENSION_OK );
        temoinSortie.close(  );
    }

    /**
     * Crée le fichier de webService (extension .ws)
     * @param strPath le repertoire dans lequel le fichier sera créé
     * @param strNameFichier le nom du fichier
     * @param strNameFichierMetier le nom du fichier metier (fichier XML)
     * @param bIsMunicipal true si la formation conseil est MUNICIPAL
     * @throws IOException IOException
     */
    private void creationFichierWebService( String strPath, String strNameFichier, String strNameFichierMetier,
        boolean bIsMunicipal ) throws IOException
    {
        String strSiren;
        String strDNUtilisateurWS;

        if ( bIsMunicipal )
        {
            strSiren = AppPropertiesService.getProperty( PROPERTY_ACTE_SIREN_VILLE );
            strDNUtilisateurWS = AppPropertiesService.getProperty( PROPERTY_ACTE_WS_DNUTILISATEUR_VILLE );
        }
        else
        {
            strSiren = AppPropertiesService.getProperty( PROPERTY_ACTE_SIREN_DEPT );
            strDNUtilisateurWS = AppPropertiesService.getProperty( PROPERTY_ACTE_WS_DNUTILISATEUR_DEPT );
        }

        String strTraitementWS = AppPropertiesService.getProperty( PROPERTY_ACTE_WS_TRAITEMENT );
        String strXMLWS = XML_ENTETE + XML_WS_ENTETE + XML_WS_BALISE_ACTE + XML_WS_BALISE_TRAITEMENT + strTraitementWS +
            XML_WS_BALISE_FERMETURE_TRAITEMENT + XML_WS_BALISE_DNUTILISATEUR + strDNUtilisateurWS +
            XML_WS_BALISE_FERMETURE_DNUTILISATEUR + XML_WS_BALISE_SIREN + strSiren + XML_WS_BALISE_FERMETURE_SIREN +
            XML_WS_BALISE_FICHIER + strNameFichierMetier + XML_WS_BALISE_FERMETURE_FICHIER +
            XML_WS_BALISE_FERMETURE_ACTE + XML_WS_FERMETURE_ENTETE;

        FileWriter wsSortie = new FileWriter( strPath + PATH_SEPARATOR + strNameFichier + EXTENSION_WS );
        wsSortie.write( strXMLWS );

        wsSortie.close(  );
    }

    /**
     * Crée le fichier de la délibéraion finale
     * @param strPath le repertoire dans lequel le fichier sera créé
     * @param strName le nom du fichier
     * @param deliberationFinal la délibération finale
     * @throws IOException IOException
     */
    private void creationFichierDeliberationFinale( String strPath, String strName, byte[] deliberationFinal )
        throws IOException
    {
        FileOutputStream fileOutputStreamdb = new FileOutputStream( strPath + PATH_SEPARATOR + strName +
                EXTENSION_PDF_PJ );
        BufferedOutputStream bufferdb = new BufferedOutputStream( fileOutputStreamdb );
        bufferdb.write( deliberationFinal );
        bufferdb.close(  );
    }

    /**
     * Crée le fichier métier (fichier XML)
     * @param strTmpPath le repertoire temporaire dans lequel le fichier sera créé
     * @param strNameFichierMetier le nom du fichier metier
     * @param strXMLMetier le flux XML
     * @throws IOException IOException
     */
    private void creationFichierMetier( String strTmpPath, String strNameFichierMetier, String strXMLMetier )
        throws IOException
    {
        String strNameFichierMetierXML = strTmpPath + PATH_SEPARATOR + strNameFichierMetier;
        FileWriter sortie = new FileWriter( strNameFichierMetierXML );
        sortie.write( strXMLMetier );
        sortie.close(  );
    }

    /**
     * Supprimer le dossier temporaire de travail
     * @param strTmpPath le repertoire temporaire à supprimer
     */
    private void supprimerRepertoireTmp( String strTmpPath )
    {
        File fileTmp = new File( strTmpPath );
        String[] fileNames = fileTmp.list(  );

        if ( fileNames != null )
        {
            for ( int i = 0; i < fileNames.length; i++ )
            {
                File file = new File( fileTmp, fileNames[i] );
                file.delete(  );
            }
        }

        fileTmp.delete(  );
    }
    
    /**
     * Retourne le tableau des caractères interdits
     * @return le tableau des caractères interdits
     */
    private String[] getDeficientCaracteres(  )
    {
    	String strDeficientCaracteres = AppPropertiesService.getProperty( PROPERTY_CARACTERES_DEFICIENT );
    	String[] strTabDeficientCaracteres = null;
    	
    	if( strDeficientCaracteres != null )
    	{
    		strTabDeficientCaracteres = strDeficientCaracteres.split( CONSTANTE_CARACTERS_DEFICIENT_SEPARATOR );
    	}
    	
    	return strTabDeficientCaracteres;
    }
    
    /**
     * Retourne la date de decision au format attendu
     * @param tsDateDecision la date de decision
     * @return la date de decision au format attendu
     */
    private String getDateDecision ( Timestamp tsDateDecision )
    {
        DateFormat format = new SimpleDateFormat( CONSTANTE_PATTERN_DATE );
        String strDateDecisionFormat = format.format( DateUtil.formatDate( DateUtil.getDateString(tsDateDecision), LOCALE ) );
    	
        return strDateDecisionFormat;
    }
}