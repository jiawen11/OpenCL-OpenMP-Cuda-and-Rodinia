/* file: PcaCorStep2Reducer.java */
/*
 //  Copyright(C) 2014-2015 Intel Corporation. All Rights Reserved.
 //
 //  The source code, information  and  material ("Material") contained herein is
 //  owned  by Intel Corporation or its suppliers or licensors, and title to such
 //  Material remains  with Intel Corporation  or its suppliers or licensors. The
 //  Material  contains proprietary information  of  Intel or  its  suppliers and
 //  licensors. The  Material is protected by worldwide copyright laws and treaty
 //  provisions. No  part  of  the  Material  may  be  used,  copied, reproduced,
 //  modified, published, uploaded, posted, transmitted, distributed or disclosed
 //  in any way  without Intel's  prior  express written  permission. No  license
 //  under  any patent, copyright  or  other intellectual property rights  in the
 //  Material  is  granted  to  or  conferred  upon  you,  either  expressly,  by
 //  implication, inducement,  estoppel or  otherwise.  Any  license  under  such
 //  intellectual  property  rights must  be express  and  approved  by  Intel in
 //  writing.
 //
 //  *Third Party trademarks are the property of their respective owners.
 //
 //  Unless otherwise  agreed  by Intel  in writing, you may not remove  or alter
 //  this  notice or  any other notice embedded  in Materials by Intel or Intel's
 //  suppliers or licensors in any way.
 */

package DAAL;

import java.io.*;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.fs.FileSystem;

import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.algorithms.pca.*;
import com.intel.daal.algorithms.PartialResult;
import com.intel.daal.data_management.data.*;
import com.intel.daal.services.*;

public class PcaCorStep2Reducer extends Reducer<IntWritable, WriteableData, IntWritable, WriteableData> {

    @Override
    public void reduce(IntWritable key, Iterable<WriteableData> values, Context context)
                  throws IOException, InterruptedException {

        DaalContext daalContext = new DaalContext();

        /* Create an algorithm to compute PCA decomposition using the correlation method on the master node */
        DistributedStep2Master pcaMaster = new DistributedStep2Master(daalContext, Double.class, Method.correlationDense);

        for (WriteableData value : values) {
            PartialResult pr = (PartialResult)value.getObject(daalContext);
            pcaMaster.input.add( MasterInputId.partialResults, pr );
        }

        /* Compute PCA decomposition on the master node */
        pcaMaster.compute();

        /* Finalize computations and retrieve the results */
        Result res = pcaMaster.finalizeCompute();

        HomogenNumericTable eigenValues  = (HomogenNumericTable)res.get(ResultId.eigenValues );
        HomogenNumericTable eigenVectors = (HomogenNumericTable)res.get(ResultId.eigenVectors);

        context.write(new IntWritable(0), new WriteableData( eigenValues  ) );
        context.write(new IntWritable(1), new WriteableData( eigenVectors ) );

        daalContext.dispose();
    }
}
