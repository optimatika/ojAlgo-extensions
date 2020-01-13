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

import org.ojalgo.ann.ArtificialNeuralNetwork;
import org.ojalgo.ann.NetworkBuilder;
import org.ojalgo.array.Primitive64Array;
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
    private ArtificialNeuralNetwork myNetwork;
    private int myNumberOfAttributes;
    private Primitive64Store myOutput;

    public NeuralClassifier() {
        super();
    }

    public void buildClassifier(Instances trainingData) throws Exception {

        myClassIndex = trainingData.classIndex();
        myClassAttribute = trainingData.attribute(myClassIndex);
        myNumberOfAttributes = trainingData.numAttributes();

        int numberOfInputs = myNumberOfAttributes - 1;
        int numberOfOutputs = myClassAttribute.numValues();

        myInput = Primitive64Array.make(numberOfInputs);
        myOutput = Primitive64Store.FACTORY.make(numberOfOutputs, 1);

        NetworkBuilder builder = ArtificialNeuralNetwork.builder(numberOfInputs, (numberOfInputs + numberOfOutputs) / 2, numberOfOutputs)
                .activators(ArtificialNeuralNetwork.Activator.TANH, ArtificialNeuralNetwork.Activator.SOFTMAX)
                .error(ArtificialNeuralNetwork.Error.CROSS_ENTROPY);

        for (Instance example : trainingData) {
            this.copyToInput(example, myInput);
            this.copyToOutput(example, myOutput);
            builder.train(myInput, myOutput);
        }

        myNetwork = builder.get();
    }

    public double classifyInstance(Instance instance) throws Exception {

        this.copyToInput(instance, myInput);

        myNetwork.invoke(myInput).supplyTo(myOutput);

        return myOutput.indexOfLargest();
    }

    public double[] distributionForInstance(Instance instance) throws Exception {

        this.copyToInput(instance, myInput);

        return myNetwork.invoke(myInput).toRawCopy1D();
    }

    public Capabilities getCapabilities() {
        return new Capabilities(this);
    }

    private void copyToInput(Instance example, Primitive64Array input) {
        for (int i = 0; i < myClassIndex; i++) {
                input.set(i, example.value(i));
        }
        for (int i = myClassIndex + 1; i < myNumberOfAttributes; i++) {
                input.set(i - 1, example.value(i));
        }
    }

    private void copyToOutput(Instance example, Primitive64Array output) {
        output.fillAll(0D);
        output.set(myClassAttribute.indexOfValue(example.stringValue(myClassIndex)), 1D);
    }

}
