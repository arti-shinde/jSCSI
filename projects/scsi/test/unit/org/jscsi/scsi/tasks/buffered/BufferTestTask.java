package org.jscsi.scsi.tasks.buffered;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Random;

import org.apache.log4j.Logger;
import org.jscsi.scsi.protocol.Command;
import org.jscsi.scsi.protocol.cdb.CDB;
import org.jscsi.scsi.protocol.inquiry.InquiryDataRegistry;
import org.jscsi.scsi.protocol.inquiry.StaticInquiryDataRegistry;
import org.jscsi.scsi.protocol.mode.ModePageRegistry;
import org.jscsi.scsi.protocol.mode.StaticModePageRegistry;
import org.jscsi.scsi.protocol.sense.exceptions.IllegalRequestException;
import org.jscsi.scsi.target.Target;
import org.jscsi.scsi.tasks.Status;
import org.jscsi.scsi.tasks.Task;
import org.jscsi.scsi.tasks.TaskAttribute;
import org.jscsi.scsi.tasks.TaskFactory;
import org.jscsi.scsi.transport.Nexus;
import org.jscsi.scsi.transport.TargetTransportPort;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

public  class BufferTestTask implements TargetTransportPort
{
   private static Logger _logger = Logger.getLogger(BufferTestTask.class);
   
   static final int STORE_CAPACITY = 1024 * 1024 * 32; // 32MB
   static final int STORE_BLOCK_SIZE = 4096;
   static final String STORE_FILE_PATH = "test-output/file-store.dat";

   private static ModePageRegistry modeRegistry;
   private static InquiryDataRegistry inquiryRegistry;
   private static ByteBuffer memBuf;
   private static ByteBuffer fileBuf;
   private static TaskFactory memFactory;
   private static TaskFactory fileFactory;
   private static RandomAccessFile file;
   

   private Random rnd = new Random();
   private HashMap<Long,byte[]> readDataMap = new HashMap<Long,byte[]>();
   private HashMap<Long,byte[]> writeDataMap = new HashMap<Long,byte[]>();
   
   @BeforeClass
   public static void setUpBeforeClass() throws Exception
   {
      _logger.debug("configuring test class");
      
      new File("test-output").mkdir();
      
      // initialize the buffers
      memBuf = ByteBuffer.allocate(STORE_CAPACITY);
      file = new RandomAccessFile(STORE_FILE_PATH, "rw");
      fileBuf = file.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, STORE_CAPACITY);

      // initialize the registries
      modeRegistry = new StaticModePageRegistry();
      inquiryRegistry = new StaticInquiryDataRegistry();
      
      memFactory = new BufferedTaskFactory(memBuf, STORE_BLOCK_SIZE, modeRegistry, inquiryRegistry);
      fileFactory = new BufferedTaskFactory(fileBuf, STORE_BLOCK_SIZE, modeRegistry, inquiryRegistry);
   }

   @AfterClass
   public static void tearDownAfterClass() throws Exception
   {
      _logger.debug("flushing file buffer to backing store");
      ((MappedByteBuffer)fileBuf).force();
      
      _logger.debug("closing file buffer backing store");
      file.close();
   }
   
   @Before
   public void setUp() throws Exception
   {
   }
   
   @After
   public void tearDown() throws Exception
   {
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // TargetTransportPort implementation
   
   @Override
   public boolean readData(Nexus nexus, long commandReferenceNumber, ByteBuffer output)
         throws InterruptedException
   {
      _logger.debug("servicing readData request: nexus: " + nexus + ", cmdRef: " + commandReferenceNumber);
      output.put(this.readDataMap.get(commandReferenceNumber));      
      return true;
   }

   @Override
   public boolean writeData(Nexus nexus, long commandReferenceNumber, ByteBuffer input)
         throws InterruptedException
   {
      _logger.debug("servicing writeData request");
      this.writeDataMap.put(commandReferenceNumber, input.array().clone());
      return true;
   }
   
   @Override
   public void registerTarget(Target target)
   {
      _logger.debug("servicing registerTarget request");
   }

   @Override
   public void removeTarget(String targetName) throws Exception
   {
      _logger.debug("servicing removeTarget request");
   }

   @Override
   public void terminateDataTransfer(Nexus nexus, long commandReferenceNumber)
   {
      _logger.debug("servicing terminateDataTransfer request");
   }

   @Override
   public void writeResponse(
         Nexus nexus,
         long commandReferenceNumber,
         Status status,
         ByteBuffer senseData)
   {
      _logger.debug("servicing writeResponse request: nexus: " + nexus + ", cmdRef: " + commandReferenceNumber);
      _logger.debug("response was status: " + status);
   }
   
   /////////////////////////////////////////////////////////////////////////////

   public byte[] createReadData(int size, long cmdRef)
   {
      byte[] data = new byte[size];
      this.rnd.nextBytes(data);
      this.readDataMap.put(cmdRef, data);
      return data;
   }
   
   public void purgeReadData(long cmdRef)
   {
      this.readDataMap.remove(cmdRef);
   }
   
   public byte[] getWriteData(long cmdRef)
   {
      return this.writeDataMap.remove(cmdRef);
   }
   
   public void submitWrite(CDB cdb, int cmdRef)
   {
      Command cmd = new Command(new Nexus("initiator", "target", 0, 0), cdb, TaskAttribute.ORDERED, cmdRef, 0);

      try
      {
         Task task = this.getMemoryTask(this, cmd);
         task.run();
         
         task = this.getFileTask(this, cmd);
         task.run();
      }
      catch (IllegalRequestException e)
      {
         Assert.fail("illegal request");
      }
   }
   
   /////////////////////////////////////////////////////////////////////////////
   
   public Task getMemoryTask(TargetTransportPort port, Command command)
   throws IllegalRequestException
   {
      return memFactory.getInstance(port, command);
   }
   
   public Task getFileTask(TargetTransportPort port, Command command)
   throws IllegalRequestException
   {
      return fileFactory.getInstance(port, command);
   }
}
