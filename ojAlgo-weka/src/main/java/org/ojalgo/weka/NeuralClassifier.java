/*
 * Copyright 1997-2020 Optimatika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.ojalgo.weka;

import static org.ojalgo.ann.ArtificialNeuralNetwork.Activator.*;
import static org.ojalgo.function.constant.PrimitiveMath.*;

import org.ojalgo.ann.ArtificialNeuralNetwork;
import org.ojalgo.ann.NetworkInvoker;
import org.ojalgo.ann.NetworkTrainer;
import org.ojalgo.array.Primitive64Array;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.Primitive64Store;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.Instance;
import weka.core.Instances;

public class NeuralClassifier implements Classifier, CapabilitiesHandler {

    private Attribute myClassAttribute;
    private int myClassIndex;
    private Primitive64Array myInput;
    private NetworkInvoker myInvoker;
    private ArtificialNeuralNetwork myNetwork;
    private int myNumberOfAttributes;
    private Primitive64Store myOutput;

    public NeuralClassifier() {
        super();
    }

    public void buildClassifier(final Instances trainingData) throws Exception {

        myClassIndex = trainingData.classIndex();
        myClassAttribute = trainingData.attribute(myClassIndex);
        myNumberOfAttributes = trainingData.numAttributes();

        int numberOfInputs = myNumberOfAttributes - 1;
        int numberOfOutputs = myClassAttribute.numValues();
        int numberOfHidden = Math.toIntExact(2 * Math.round(Math.sqrt(numberOfInputs * numberOfOutputs)));

        myInput = Primitive64Array.make(numberOfInputs);
        myOutput = Primitive64Store.FACTORY.make(numberOfOutputs, 1);

        myNetwork = ArtificialNeuralNetwork.builder(numberOfInputs).layer(numberOfHidden, RECTIFIER).layer(numberOfHidden, SIGMOID)
                .layer(numberOfOutputs, SOFTMAX).get();

        NetworkTrainer trainer = myNetwork.newTrainer().dropouts().rate(ONE / Math.sqrt(trainingData.size()));

        for (Instance example : trainingData) {
            this.copyToInput(example, myInput);
            this.copyToOutput(example, myOutput);
            trainer.train(myInput, myOutput);
        }
    }

    public double classifyInstance(final Instance instance) throws Exception {

        MatrixStore<Double> output = this.invoke(instance);

        output.supplyTo(myOutput);

        return myOutput.indexOfLargest();
    }

    public double[] distributionForInstance(final Instance instance) throws Exception {

        MatrixStore<Double> output = this.invoke(instance);

        return output.toRawCopy1D();
    }

    public Capabilities getCapabilities() {
        return new Capabilities(this);
    }

    private void copyToInput(final Instance example, final Primitive64Array input) {
        for (int i = 0; i < myClassIndex; i++) {
            input.set(i, example.value(i));
        }
        for (int i = myClassIndex + 1; i < myNumberOfAttributes; i++) {
            input.set(i - 1, example.value(i));
        }
    }

    private void copyToOutput(final Instance example, final Primitive64Array output) {
        output.fillAll(0D);
        output.set(myClassAttribute.indexOfValue(example.stringValue(myClassIndex)), 1D);
    }

    private MatrixStore<Double> invoke(final Instance instance) {

        this.copyToInput(instance, myInput);

        if (myInvoker == null) {
            myInvoker = myNetwork.newInvoker();
        }

        return myInvoker.invoke(myInput);
    }

}
