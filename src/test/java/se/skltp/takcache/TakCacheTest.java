package se.skltp.takcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static se.skltp.takcache.TakCacheLog.RefreshStatus.REFRESH_OK;
import static se.skltp.takcache.TakCacheLog.RefreshStatus.RESTORED_FROM_LOCAL_CACHE;
import static se.skltp.takcache.util.TestTakDataDefines.ADDRESS_1;
import static se.skltp.takcache.util.TestTakDataDefines.NAMNRYMD_1;
import static se.skltp.takcache.util.TestTakDataDefines.NAMNRYMD_2;
import static se.skltp.takcache.util.TestTakDataDefines.RECEIVER_1;
import static se.skltp.takcache.util.TestTakDataDefines.RECEIVER_2;
import static se.skltp.takcache.util.TestTakDataDefines.RIV20;
import static se.skltp.takcache.util.TestTakDataDefines.RIV21;
import static se.skltp.takcache.util.TestTakDataDefines.SENDER_1;
import static se.skltp.takcache.util.TestTakDataDefines.SENDER_3;

import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.InputSource;
import se.skltp.takcache.services.TakService;
import se.skltp.takcache.util.VagvalSchemasTestListsUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath*:spring-context.xml")
@TestPropertySource("classpath:test-properties.properties")
public class TakCacheTest {

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Mock
  TakService takService;

  @InjectMocks
  @Autowired
  private TakCacheImpl takCache;

  @BeforeClass
  public static void beforeClass() {

  }

  @Before
  public void beforeTest() {
    MockitoAnnotations.initMocks(this);

    // Reset internal cache between tests
    takCache.behorighetCache=null;
    takCache.vagvalCache=null;
    takCache.tjanstegranssnittFilter=null;
    takCache.setLocalCacheFileNames(null, null);
  }

  @Test
  public void simpleSuccessfulRefreshTest() throws Exception {
    Mockito.when(takService.getBehorigheter())
        .thenReturn(VagvalSchemasTestListsUtil.getStaticBehorighetList());
    Mockito.when(takService.getVirtualiseringar())
        .thenReturn(VagvalSchemasTestListsUtil.getStaticVagvalList());
    TakCacheLog takCacheLog = takCache.refresh();

    assertTrue(takCacheLog.isRefreshSuccessful());
    assertEquals(REFRESH_OK, takCacheLog.getBehorigheterRefreshStatus());
    assertEquals(REFRESH_OK, takCacheLog.getVagvalRefreshStatus());
    assertEquals(5, takCacheLog.getNumberBehorigheter());
    assertEquals(5, takCacheLog.getNumberVagval());
  }

  @Test
  public void simpleBehorighetTest() throws Exception {
    Mockito.when(takService.getBehorigheter())
        .thenReturn(VagvalSchemasTestListsUtil.getStaticBehorighetList());
    assertTrue(takCache.isAuthorized(SENDER_1, NAMNRYMD_1, RECEIVER_1));
    assertTrue(takCache.isAuthorized(SENDER_1, NAMNRYMD_2, RECEIVER_1));
    assertFalse(takCache.isAuthorized(SENDER_3, NAMNRYMD_1, RECEIVER_1));
  }

  @Test
  public void filterShouldRemoveNotMatchingBehorighetTest() throws Exception {
    Mockito.when(takService.getBehorigheter())
        .thenReturn(VagvalSchemasTestListsUtil.getStaticBehorighetList());

    TakCacheLog takCacheLog = takCache.refresh(NAMNRYMD_1);
    assertEquals(4, takCacheLog.getNumberBehorigheter());
    assertTrue(takCache.isAuthorized(SENDER_1, NAMNRYMD_1, RECEIVER_1));
    assertFalse(takCache.isAuthorized(SENDER_1, NAMNRYMD_2, RECEIVER_1));
  }

  @Test
  public void simpleVagvalTest() throws Exception {
    Mockito.when(takService.getVirtualiseringar())
        .thenReturn(VagvalSchemasTestListsUtil.getStaticVagvalList());
    assertEquals(0, takCache.getRoutingInfo(NAMNRYMD_1, RECEIVER_1).size());
    assertEquals(1, takCache.getRoutingInfo(NAMNRYMD_1, RECEIVER_2).size());
    assertEquals(2, takCache.getRoutingInfo(NAMNRYMD_2, RECEIVER_2).size());
    assertEquals(ADDRESS_1, takCache.getRoutingAddress(NAMNRYMD_1, RECEIVER_2, RIV21));
    assertEquals(ADDRESS_1, takCache.getRoutingAddress(NAMNRYMD_2, RECEIVER_2, RIV20));
  }


  @Test
  public void filterShouldRemoveNotMatchingNamespaceInVagvalTest() throws Exception {
    Mockito.when(takService.getVirtualiseringar())
        .thenReturn(VagvalSchemasTestListsUtil.getStaticVagvalList());
    TakCacheLog takCacheLog = takCache.refresh(NAMNRYMD_1);
    assertEquals(REFRESH_OK, takCacheLog.getVagvalRefreshStatus());
    assertEquals(3, takCacheLog.getNumberVagval());
    assertEquals(0, takCache.getRoutingInfo(NAMNRYMD_2, RECEIVER_2).size());
  }

  @Test
  public void filterShouldNotEffectRemainingVagvalTest() throws Exception {
    Mockito.when(takService.getVirtualiseringar())
        .thenReturn(VagvalSchemasTestListsUtil.getStaticVagvalList());
    TakCacheLog takCacheLog = takCache.refresh(NAMNRYMD_1);

    assertEquals(0, takCache.getRoutingInfo(NAMNRYMD_1, RECEIVER_1).size());
    assertEquals(1, takCache.getRoutingInfo(NAMNRYMD_1, RECEIVER_2).size());
    assertEquals(ADDRESS_1, takCache.getRoutingAddress(NAMNRYMD_1, RECEIVER_2, RIV21));
  }
  @Test
  public void noContactWithTjanstekatalogenAlwaysReultsInLocalCacheIsRead()
      throws Exception {

    URL localTakVagvalCache = TakCacheTest.class.getClassLoader()
        .getResource("tklocalcache-vagval-test.xml");
    URL localTakBehorigetCache = TakCacheTest.class.getClassLoader()
        .getResource("tklocalcache-behorighet-test.xml");
    takCache
        .setLocalCacheFileNames(localTakBehorigetCache.getFile(), localTakVagvalCache.getFile());

    Mockito.when(takService.getVirtualiseringar())
        .thenThrow(new Exception("Failed get virtualizations from TAK"));
    Mockito.when(takService.getBehorigheter())
        .thenThrow(new Exception("Failed get behorigheter from TAK"));

    TakCacheLog takCacheLog = takCache.refresh();

    assertFalse(takCacheLog.isRefreshSuccessful());
    assertEquals(RESTORED_FROM_LOCAL_CACHE, takCacheLog.getBehorigheterRefreshStatus());
    assertEquals(RESTORED_FROM_LOCAL_CACHE, takCacheLog.getVagvalRefreshStatus());
    assertEquals(1, takCacheLog.getNumberBehorigheter());
    assertEquals(1, takCacheLog.getNumberVagval());

  }

  @Test
  public void emptyAnswerFromTjanstekatalogenAlwaysReultsInLocalCacheIsRead()
      throws Exception {

    URL localTakVagvalCache = TakCacheTest.class.getClassLoader()
        .getResource("tklocalcache-vagval-test.xml");
    URL localTakBehorigetCache = TakCacheTest.class.getClassLoader()
        .getResource("tklocalcache-behorighet-test.xml");
    takCache
        .setLocalCacheFileNames(localTakBehorigetCache.getFile(), localTakVagvalCache.getFile());

    Mockito.when(takService.getVirtualiseringar()).thenReturn(new ArrayList<>());
    Mockito.when(takService.getBehorigheter()).thenReturn(new ArrayList<>());

    TakCacheLog takCacheLog = takCache.refresh();

    assertFalse(takCacheLog.isRefreshSuccessful());
    assertEquals(RESTORED_FROM_LOCAL_CACHE, takCacheLog.getBehorigheterRefreshStatus());
    assertEquals(RESTORED_FROM_LOCAL_CACHE, takCacheLog.getVagvalRefreshStatus());
    assertEquals(1, takCacheLog.getNumberBehorigheter());
    assertEquals(1, takCacheLog.getNumberVagval());

  }


  @Test
  public void noDataAfterFilterShouldReultsInLocalCacheIsRead()
      throws Exception {

    URL localTakVagvalCache = TakCacheTest.class.getClassLoader()
        .getResource("tklocalcache-vagval-test.xml");
    URL localTakBehorigetCache = TakCacheTest.class.getClassLoader()
        .getResource("tklocalcache-behorighet-test.xml");
    takCache
        .setLocalCacheFileNames(localTakBehorigetCache.getFile(), localTakVagvalCache.getFile());

    Mockito.when(takService.getVirtualiseringar()).thenReturn(VagvalSchemasTestListsUtil.getStaticVagvalList());
    Mockito.when(takService.getBehorigheter()).thenReturn(VagvalSchemasTestListsUtil.getStaticBehorighetList());

    TakCacheLog takCacheLog = takCache.refresh("NamespaceNoMatch");

    assertFalse(takCacheLog.isRefreshSuccessful());
    assertEquals(RESTORED_FROM_LOCAL_CACHE, takCacheLog.getBehorigheterRefreshStatus());
    assertEquals(RESTORED_FROM_LOCAL_CACHE, takCacheLog.getVagvalRefreshStatus());
    assertEquals(1, takCacheLog.getNumberBehorigheter());
    assertEquals(1, takCacheLog.getNumberVagval());

  }

  @Test
  public void contactWithTjanstekatalogenAlwaysReultsInLocalCacheIsUpdated()
      throws Exception {

    String vagValfileName = String
        .format("%s/vagvalcache-test.xml", testFolder.getRoot().getPath());
    String behorigheterfileName = String
        .format("%s/behorighetercache-test.xml", testFolder.getRoot().getPath());
    takCache.setLocalCacheFileNames(behorigheterfileName, vagValfileName);

    Mockito.when(takService.getVirtualiseringar())
        .thenReturn(VagvalSchemasTestListsUtil.getStaticVagvalList());
    Mockito.when(takService.getBehorigheter())
        .thenReturn(VagvalSchemasTestListsUtil.getStaticBehorighetList());

    TakCacheLog takCacheLog = takCache.refresh();

    assertTrue(takCacheLog.isRefreshSuccessful());
    assertEquals(REFRESH_OK, takCacheLog.getBehorigheterRefreshStatus());
    assertEquals(REFRESH_OK, takCacheLog.getVagvalRefreshStatus());
    XMLAssert.assertXpathExists("/persistentCache/virtualiseringsInfo",
        new InputSource(new FileReader(vagValfileName)));
    XMLAssert.assertXpathExists("/persistentCache/anropsBehorighetsInfo",
        new InputSource(new FileReader(behorigheterfileName)));
  }


}