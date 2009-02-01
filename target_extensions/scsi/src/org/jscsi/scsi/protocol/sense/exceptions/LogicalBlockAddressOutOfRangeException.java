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

package org.jscsi.scsi.protocol.sense.exceptions;

import org.jscsi.scsi.protocol.sense.KCQ;
import org.jscsi.scsi.protocol.sense.additional.FieldPointer;

//TODO: Describe class or interface
public class LogicalBlockAddressOutOfRangeException extends IllegalRequestException
{
   private static final long serialVersionUID = -6208018053285032168L;

   private FieldPointer fieldPointer;

   /**
    * Indicates an LBA value was out of range.
    * @param current True if the error is current; False if the error is deferred.
    * @param command True if the error occurred in a CDB; False if the error was in parameter data.
    * @param bitPointer The first bit where an error occurs; bit value indicated in MSB
    *    (left-most) form.
    * @param fieldPointer The first byte where an error occurs.
    */
   public LogicalBlockAddressOutOfRangeException(
         boolean current,
         boolean command,
         byte bitPointer,
         int fieldPointer)
   {
      super(KCQ.LOGICAL_BLOCK_ADDRESS_OUT_OF_RANGE, current);
      assert bitPointer <= 0x07 : "bit pointer value out of range";
      assert fieldPointer <= 65536 : "field pointer value out of range";

      this.fieldPointer = new FieldPointer(command, bitPointer, fieldPointer);
   }

   /**
    * Indicates an LBA value was out of range.
    * @param current True if the error is current; False if the error is deferred.
    * @param command True if the error occurred in a CDB; False if the error was in parameter data.
    * @param fieldPointer The first byte where an error occurs.
    */
   public LogicalBlockAddressOutOfRangeException(boolean current, boolean command, int fieldPointer)
   {
      super(KCQ.LOGICAL_BLOCK_ADDRESS_OUT_OF_RANGE, current);
      assert fieldPointer <= 65536 : "field pointer value out of range";

      this.fieldPointer = new FieldPointer(command, (byte) -1, fieldPointer);

   }

   @Override
   protected FieldPointer getFieldPointer()
   {
      return this.fieldPointer;
   }

}
