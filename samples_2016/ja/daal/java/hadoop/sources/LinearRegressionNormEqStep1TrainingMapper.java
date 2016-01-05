/* file: LinearRegressionNormEqStep1TrainingMapper.java */
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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.conf.Configuration;

import com.intel.daal.data_management.data.HomogenNumericTable;
import com.intel.daal.algorithms.linear_regression.Model;
import com.intel.daal.algorithms.linear_regression.training.*;
import com.intel.daal.data_management.data.*;
import com.intel.daal.services.*;

public class LinearRegressionNormEqStep1TrainingMapper extends Mapper<Object, Text, IntWritable, WriteableData> {

    private static final int nDataFeatures   = 10;
    private static final int nLabelsFeatures = 2;
    private static final int nVectorsInBlock = 250;

    /* Index is supposed to be a sequence number for the split */
    private int index = 0;
    private int totalTasks = 0;

    @Override
    public void setup(Context context) {
        index = context.getTaskAttemptID().getTaskID().getId();
        Configuration conf = context.getConfiguration();
        totalTasks = conf.getInt("mapred.map.tasks", 0);
    }

    @Override
    public void map(Object key, Text value,
                    Context context) throws IOException, InterruptedException {

        /* Read a data set */
        String dataFilePath = "/Hadoop/LinearRegressionNormEq/data/" + value + "_train.csv";
        String labelsFilePath = "/Hadoop/LinearRegressionNormEq/data/" + value + "_train_labels.csv";

        double[] data = new double[nDataFeatures * nVectorsInBlock];
        double[] labels = new double[nLabelsFeatures * nVectorsInBlock];

        readData(dataFilePath, nDataFeatures, nVectorsInBlock, data);
        readData(labelsFilePath, nLabelsFeatures, nVectorsInBlock, labels);

        DaalContext daalContext = new DaalContext();

        HomogenNumericTable ntData = new HomogenNumericTable(daalContext, data, nDataFeatures, nVectorsInBlock);
        HomogenNumericTable ntLabels = new HomogenNumericTable(daalContext, labels, nLabelsFeatures, nVectorsInBlock);

        /* Create an algorithm object to train the multiple linear regression model with the normal equations method */
        TrainingDistributedStep1Local linearRegressionTraining = new TrainingDistributedStep1Local(daalContext,
                                                                                                   Double.class, TrainingMethod.normEqDense);
        /* Set the input data */
        linearRegressionTraining.input.set(TrainingInputId.data, ntData);
        linearRegressionTraining.input.set(TrainingInputId.dependentVariable, ntLabels);

        /* Build a partial multiple linear regression model */
        PartialResult pres = linearRegressionTraining.compute();

        /* Write the data prepended with a data set sequence number. Needed to know the position of the data set in the input data */
        context.write(new IntWritable(0), new WriteableData(index, pres));
        index += totalTasks;

        daalContext.dispose();
    }

    private static void readData(String dataset, int nFeatures, int nVectors, double[] data) {
        System.out.println("readData " + dataset);
        try {
            Path pt = new Path(dataset);
            FileSystem fs = FileSystem.get(new Configuration());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fs.open(pt)));

            int nLine = 0;
            for (String line; ((line = bufferedReader.readLine()) != null) && (nLine < nVectors); nLine++) {
                String[] elements = line.split(",");
                for (int j = 0; j < nFeatures; j++) {
                    data[nLine * nFeatures + j] = Double.parseDouble(elements[j]);
                }
            }
            bufferedReader.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
