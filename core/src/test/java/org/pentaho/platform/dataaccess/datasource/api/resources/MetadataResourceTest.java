/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.platform.dataaccess.datasource.api.resources;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.PentahoAccessControlException;
import org.pentaho.platform.dataaccess.datasource.api.MetadataService;
import org.pentaho.platform.plugin.services.importer.PlatformImportException;
import org.pentaho.platform.api.repository2.unified.webservices.RepositoryFileAclDto;
import org.pentaho.platform.web.http.api.resources.FileResource;
import org.pentaho.platform.web.http.api.resources.JaxbList;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;

public class MetadataResourceTest {

  private static MetadataResource metadataResource;

  private class MetadataResourceMock extends MetadataResource {
    @Override protected MetadataService createMetadataService() {
      return mock( MetadataService.class );
    }
  }

  @Before
  public void setUp() {
    metadataResource = spy( new MetadataResourceMock() );
  }

  @After
  public void cleanup() {
    metadataResource = null;
  }

  @Test
  public void testdownloadMetadata() throws Exception {
    Response mockResponse = mock( Response.class );
    Map<String, InputStream> mockFileData = mock( Map.class );

    doReturn( true ).when( metadataResource ).canAdminister();
    doReturn( true ).when( metadataResource )
      .isInstanceOfIPentahoMetadataDomainRepositoryExporter( metadataResource.metadataDomainRepository );
    doReturn( mockFileData ).when( metadataResource ).getDomainFilesData( "metadataId" );
    doReturn( mockResponse ).when( metadataResource ).createAttachment( mockFileData, "metadataId" );

    Response response = metadataResource.downloadMetadata( "metadataId" );

    verify( metadataResource, times( 1 ) ).downloadMetadata( "metadataId" );
    assertEquals( mockResponse, response );
  }

  @Test
  public void testdownloadMetadataError() throws Exception {
    Response mockResponse = mock( Response.class );

    //Test 1
    doReturn( false ).when( metadataResource ).canAdminister();
    doReturn( mockResponse ).when( metadataResource ).buildUnauthorizedResponse();

    try {
      Response response = metadataResource.downloadMetadata( "metadataId" );
      fail( "Should have gotten a WebApplicationException" );
    } catch ( WebApplicationException e ) {
      Assert.assertEquals( 401, e.getResponse().getStatus() );
    }

    //Test 2
    doReturn( true ).when( metadataResource ).canAdminister();
    doReturn( mockResponse ).when( metadataResource ).buildServerErrorResponse();

    try {
      Response response = metadataResource.downloadMetadata( "metadataId" );
      fail( "Should have gotten a WebApplicationException" );
    } catch ( WebApplicationException e ) {
      Assert.assertEquals( 500, e.getResponse().getStatus() );
    }

    verify( metadataResource, times( 2 ) ).downloadMetadata( "metadataId" );
  }

  @Test
  public void testDoRemoveMetadata() throws Exception {
    Response mockResponse = mock( Response.class );
    doReturn( mockResponse ).when( metadataResource ).buildOkResponse();

    Response response = metadataResource.deleteMetadata( "metadataId" );

    verify( metadataResource, times( 1 ) ).deleteMetadata( "metadataId" );
    assertEquals( mockResponse, response );
  }

  @Test
  public void testDoRemoveMetadataError() throws Exception {
    Response mockResponse = mock( Response.class );
    PentahoAccessControlException mockPentahoAccessControlException = mock( PentahoAccessControlException.class );
    doThrow( mockPentahoAccessControlException ).when( metadataResource.service ).removeMetadata( "metadataId" );
    doReturn( mockResponse ).when( metadataResource ).buildUnauthorizedResponse();

    try {
      Response response = metadataResource.deleteMetadata( "metadataId" );
      fail( "Should have had a WebApplicationException" );
    } catch ( WebApplicationException e ) {
      // Good
      assertEquals( 401, e.getResponse().getStatus() );
    }

    verify( metadataResource, times( 1 ) ).deleteMetadata( "metadataId" );

  }

  @Test
  public void testGetMetadataDatasourceIds() throws Exception {
    List<String> mockDSWDatasourceIds = mock( List.class );
    JaxbList<String> mockJaxbList = mock( JaxbList.class );
    doReturn( mockDSWDatasourceIds ).when( metadataResource.service ).getMetadataDatasourceIds();
    doReturn( mockJaxbList ).when( metadataResource ).createNewJaxbList( mockDSWDatasourceIds );

    JaxbList<String> response = metadataResource.listDomains();

    verify( metadataResource, times( 1 ) ).listDomains();
    assertEquals( mockJaxbList, response );
  }

  @Test
  public void testImportMetadataDatasource() throws Exception {
    Response mockResponse = Response.ok().status( new Integer( 3 ) ).type( MediaType.TEXT_PLAIN ).build();

    String domainId = "domainId";
    InputStream metadataFile = mock( InputStream.class );
    FormDataContentDisposition metadataFileInfo = mock( FormDataContentDisposition.class );
    String overwrite = "overwrite";
    List<FormDataBodyPart> localeFiles = mock( List.class );
    List<FormDataContentDisposition> localeFilesInfo = mock( List.class );

    doNothing().when( metadataResource.service )
      .importMetadataDatasource( domainId, metadataFile, metadataFileInfo, true, localeFiles,
        localeFilesInfo, null );
    doReturn( mockResponse ).when( metadataResource ).buildOkResponse( "3" );

    Response response = metadataResource.importMetadataDatasourceLegacy( domainId, metadataFile, metadataFileInfo,
      overwrite, localeFiles,
      localeFilesInfo, null );

    verify( metadataResource, times( 1 ) ).importMetadataDatasourceLegacy( domainId, metadataFile, metadataFileInfo,
      overwrite, localeFiles,
      localeFilesInfo, null );
    assertEquals( mockResponse.getStatus(), response.getStatus() );
  }

  @Test
  public void testImportMetadataDatasourceError() throws Exception {
    Response mockResponse = mock( Response.class );
    FileResource mockFileResource = mock( FileResource.class );

    String domainId = "domainId";
    InputStream metadataFile = mock( InputStream.class );
    FormDataContentDisposition metadataFileInfo = mock( FormDataContentDisposition.class );
    String overwrite = "overwrite";
    List<FormDataBodyPart> localeFiles = mock( List.class );
    List<FormDataContentDisposition> localeFilesInfo = mock( List.class );

    //Test 1
    PentahoAccessControlException mockPentahoAccessControlException = mock( PentahoAccessControlException.class );
    doThrow( mockPentahoAccessControlException ).when( metadataResource.service )
      .importMetadataDatasource( domainId, metadataFile, metadataFileInfo, false, localeFiles,
        localeFilesInfo, null );
    doReturn( mockResponse ).when( metadataResource ).buildServerErrorResponse( mockPentahoAccessControlException );

    Response response = metadataResource.importMetadataDatasourceLegacy( domainId, metadataFile, metadataFileInfo,
      overwrite, localeFiles,
      localeFilesInfo, null );
    assertEquals( mockResponse, response );

    //Test 2
    PlatformImportException mockPlatformImportException = mock( PlatformImportException.class );
    doThrow( mockPlatformImportException ).when( metadataResource.service )
      .importMetadataDatasource( domainId, metadataFile, metadataFileInfo, true, localeFiles,
        localeFilesInfo, null );
    doReturn( 10 ).when( mockPlatformImportException ).getErrorStatus();
    doReturn( mockFileResource ).when( metadataResource ).createFileResource();
    doReturn( mockResponse ).when( metadataResource ).buildServerError003Response( domainId, mockFileResource );

    response = metadataResource.importMetadataDatasourceLegacy( domainId, metadataFile, metadataFileInfo, overwrite,
      localeFiles,
      localeFilesInfo, null );
    assertEquals( mockResponse, response );

    //Test 3
    RuntimeException mockException = mock( RuntimeException.class );
    doThrow( mockPlatformImportException ).when( metadataResource.service )
      .importMetadataDatasource( domainId, metadataFile, metadataFileInfo, true, localeFiles,
        localeFilesInfo, null );
    doReturn( 1 ).when( mockPlatformImportException ).getErrorStatus();
    doReturn( mockResponse ).when( metadataResource ).buildOkResponse( "1" );
    doReturn( mockException ).when( mockPlatformImportException ).getCause();

    response = metadataResource.importMetadataDatasourceLegacy( domainId, metadataFile, metadataFileInfo, overwrite,
      localeFiles,
      localeFilesInfo, null );
    assertEquals( mockResponse, response );

    //Test
    doThrow( mockException ).when( metadataResource.service )
      .importMetadataDatasource( domainId, metadataFile, metadataFileInfo, true, localeFiles,
        localeFilesInfo, null );
    doReturn( mockResponse ).when( metadataResource ).buildServerError001Response();

    response = metadataResource.importMetadataDatasourceLegacy( domainId, metadataFile, metadataFileInfo, overwrite,
      localeFiles,
      localeFilesInfo, null );
    assertEquals( mockResponse, response );

    verify( metadataResource, times( 4 ) ).importMetadataDatasourceLegacy( domainId, metadataFile, metadataFileInfo,
      overwrite, localeFiles,
      localeFilesInfo, null );
  }

  @Test
  public void doGetMetadataAcl() throws Exception {
    String domainId = "domainId";

    doReturn( new HashMap<String, InputStream>() {
      {
        put( "test", null );
      }
    } ).when( metadataResource )
      .getDomainFilesData( domainId );

    doReturn( new RepositoryFileAclDto() ).when( metadataResource.service ).getMetadataAcl( domainId );

    metadataResource.doGetMetadataAcl( domainId ); // no exception thrown

    //
    doThrow( new PentahoAccessControlException() ).when( metadataResource.service ).getMetadataAcl( domainId );

    try {
      metadataResource.doGetMetadataAcl( domainId );
      fail();
    } catch ( WebApplicationException e ) {
      assertEquals( Response.Status.UNAUTHORIZED.getStatusCode(), e.getResponse().getStatus() );
    }

    //
    doThrow( new FileNotFoundException() ).when( metadataResource.service ).getMetadataAcl( domainId );

    try {
      metadataResource.doGetMetadataAcl( domainId );
      fail();
    } catch ( WebApplicationException e ) {
      assertEquals( Response.Status.CONFLICT.getStatusCode(), e.getResponse().getStatus() );
    }
  }

  @Test
  public void doSetMetadataAcl() throws Exception {
    String domainId = "domainId";

    doReturn( new HashMap<String, InputStream>() {
      {
        put( "test", null );
      }
    } ).when( metadataResource )
      .getDomainFilesData( domainId );

    Response response = metadataResource.doSetMetadataAcl( domainId, null );
    assertEquals( Response.Status.OK.getStatusCode(), response.getStatus() );

    //
    doThrow( new PentahoAccessControlException() ).when( metadataResource.service ).setMetadataAcl( domainId, null );

    response = metadataResource.doSetMetadataAcl( domainId, null );
    assertEquals( Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus() );

    //
    doThrow( new FileNotFoundException() ).when( metadataResource.service ).setMetadataAcl( domainId, null );

    response = metadataResource.doSetMetadataAcl( domainId, null );
    assertEquals( Response.Status.CONFLICT.getStatusCode(), response.getStatus() );
  }

  @Test
  public void importMetadataFromTemp() throws Exception {
    Response mockResponse = Response.ok().status( new Integer( 200 ) ).type( MediaType.TEXT_PLAIN ).build();

    String domainId = "domainId";
    Boolean overwrite = true;
    String localeFiles = "{xmiFileName : filename }";
    MetadataTempFilesListDto metaDto = new MetadataTempFilesListDto( );
    metaDto.setXmiFileName( "fileName" );
    System.out.println(metaDto);

    doNothing().when( metadataResource.service ).importMetadataFromTemp(
      domainId, new MetadataTempFilesListDto( localeFiles ), overwrite, null );
    doReturn( mockResponse ).when( metadataResource ).buildOkResponse( "200" );

    Response response = metadataResource.importMetadataFromTemp( domainId, localeFiles, overwrite, null );

    verify( metadataResource, times( 1 ) ).importMetadataFromTemp( domainId, localeFiles, overwrite, null );
    assertEquals( mockResponse.getStatus(), response.getStatus() );
  }

  @Test
  public void isContainsModel() throws Exception {
    Response mockResponse = Response.ok().status( new Integer( 200 ) ).type( MediaType.TEXT_PLAIN ).build();

    String tempFileName = "overwrite";

    doReturn(true).when( metadataResource.service ).isContainsModel( tempFileName );
    doReturn( mockResponse ).when( metadataResource ).buildOkResponse( "200" );

    Response response = metadataResource.isContainsModel( tempFileName );

    verify( metadataResource, times( 1 ) ).isContainsModel( tempFileName );
    assertEquals( mockResponse.getStatus(), response.getStatus() );
  }

  @Test
  public void uploadMetadataFilesToTempDir() throws Exception {
    Response mockResponse = Response.ok().status( new Integer( 3 ) ).type( MediaType.TEXT_PLAIN ).build();
    MetadataTempFilesListDto metaDto = new MetadataTempFilesListDto( );

    InputStream metadataFile = mock( InputStream.class );
    FormDataContentDisposition schemaFileInfo = mock( FormDataContentDisposition.class );
    List<FormDataBodyPart> localeFiles = mock( List.class );
    List<FormDataContentDisposition> localeFilesInfo = mock( List.class );

    doReturn(metaDto).when( metadataResource.service ).uploadMetadataFilesToTempDir( metadataFile, schemaFileInfo, localeFiles );
    doReturn( mockResponse ).when( metadataResource ).buildOkResponse( "3" );
    String res = metadataResource.uploadMetadataFilesToTempDir( metadataFile, schemaFileInfo, localeFiles );

    verify( metadataResource, times( 1 ) ).uploadMetadataFilesToTempDir( metadataFile, schemaFileInfo, localeFiles );
    assertEquals( metaDto.toJSONString(), res );
  }
}

