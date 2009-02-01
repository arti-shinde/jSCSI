//Cleversafe open-source code header - Version 1.1 - December 1, 2006
//
//Cleversafe Dispersed Storage(TM) is software for secure, private and
//reliable storage of the world's data using information dispersal.
//
//Copyright (C) 2005-2007 Cleversafe, Inc.
//
//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 2
//of the License, or (at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
//USA.
//
//Contact Information: 
// Cleversafe, 10 W. 35th Street, 16th Floor #84,
// Chicago IL 60616
// email: licensing@cleversafe.org
//
//END-OF-HEADER
//-----------------------
//@author: John Quigley <jquigley@cleversafe.com>
//@date: January 1, 2008
//---------------------

package org.jscsi.scsi.protocol.cdb;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jscsi.scsi.protocol.util.ByteBufferInputStream;

public class Read6 extends AbstractTransferCDB
{
   public static final int OPERATION_CODE = 0x08;

   public Read6()
   {
      super(OPERATION_CODE);
   }

   public Read6(boolean linked, boolean normalACA, long logicalBlockAddress, long transferLength)
   {
      super(OPERATION_CODE, linked, normalACA, logicalBlockAddress, transferLength);
      if (transferLength > 256)
      {
         throw new IllegalArgumentException("Transfer length out of bounds for command type");
      }
      if (logicalBlockAddress > 2097152)
      {
         throw new IllegalArgumentException("Logical Block Address out of bounds for command type");
      }
   }

   public Read6(long logicalBlockAddress, long transferLength)
   {
      this(false, false, logicalBlockAddress, transferLength);
   }

   public void decode(byte[] header, ByteBuffer input) throws IOException
   {
      DataInputStream in = new DataInputStream(new ByteBufferInputStream(input));

      int operationCode = in.readUnsignedByte();

      long msb = in.readUnsignedByte() & 0x1F;
      long lss = in.readUnsignedShort();
      setLogicalBlockAddress((msb << 16) | lss);

      setTransferLength(in.readUnsignedByte());
      super.setControl(in.readUnsignedByte());

      if (getTransferLength() == 0)
      {
         setTransferLength(256);
      }

      if (operationCode != OPERATION_CODE)
      {
         throw new IOException("Invalid operation code: " + Integer.toHexString(operationCode));
      }
   }

   public byte[] encode()
   {
      ByteArrayOutputStream cdb = new ByteArrayOutputStream(this.size());
      DataOutputStream out = new DataOutputStream(cdb);

      try
      {
         out.writeByte(OPERATION_CODE);

         int msb = (int) (getLogicalBlockAddress() >>> 16) & 0x1F;
         int lss = (int) getLogicalBlockAddress() & 0xFFFF;
         out.writeByte(msb);
         out.writeShort(lss);
         out.writeByte((int) getTransferLength());
         out.writeByte(super.getControl());

         return cdb.toByteArray();
      }
      catch (IOException e)
      {
         throw new RuntimeException("Unable to encode CDB.");
      }
   }

   public int size()
   {
      return 6;
   }

   @Override
   public String toString()
   {
      return "<Read6>";
   }
}
