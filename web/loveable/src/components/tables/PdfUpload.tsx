import React, {useState, useRef} from 'react';
import {Button} from '@/components/ui/button';
import {Upload, FileText, Loader2, X} from 'lucide-react';
import {useApi} from '@/hooks/useApi';
import {tablesApi} from '@/lib/api';
import {useToast} from '@/hooks/use-toast';
import {Card} from '@/components/ui/card';

interface PdfUploadProps {
    tableId: string;
    versionNumber: number;
    onRowGenerated: () => void;
}

const PdfUpload: React.FC<PdfUploadProps> = ({tableId, versionNumber, onRowGenerated}) => {
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [isUploading, setIsUploading] = useState(false);
    const [isDragging, setIsDragging] = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);
    const api = useApi();
    const {toast} = useToast();

    const handleFileSelect = (file: File) => {
        if (file.type !== 'application/pdf') {
            toast({
                title: 'Invalid File',
                description: 'Please select a PDF file',
                variant: 'destructive',
            });
            return;
        }
        setSelectedFile(file);
    };

    const handleDrop = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDragging(false);
        const file = e.dataTransfer.files[0];
        if (file) handleFileSelect(file);
    };

    const handleUpload = async () => {
        if (!selectedFile) return;

        setIsUploading(true);
        try {
            await tablesApi.generateRowFromPdf(api, tableId, versionNumber, selectedFile);
            toast({
                title: 'Processing Started',
                description: 'Your PDF is being processed. The extracted data will appear shortly.',
            });
            setSelectedFile(null);
            onRowGenerated();
        } catch (error) {
            toast({
                title: 'Upload Failed',
                description: 'Failed to process PDF. Please try again.',
                variant: 'destructive',
            });
        } finally {
            setIsUploading(false);
        }
    };

    return (
        <Card
            className={`border-2 border-dashed p-6 transition-colors ${
                isDragging ? 'border-primary bg-primary/5' : 'border-border'
            }`}
            onDragOver={(e) => {
                e.preventDefault();
                setIsDragging(true);
            }}
            onDragLeave={() => setIsDragging(false)}
            onDrop={handleDrop}
        >
            <input
                type="file"
                accept=".pdf"
                ref={fileInputRef}
                className="hidden"
                onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) handleFileSelect(file);
                }}
            />

            {selectedFile ? (
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <FileText className="h-8 w-8 text-primary"/>
                        <div>
                            <p className="font-medium text-foreground">{selectedFile.name}</p>
                            <p className="text-sm text-muted-foreground">
                                {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
                            </p>
                        </div>
                    </div>
                    <div className="flex items-center gap-2">
                        <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => setSelectedFile(null)}
                            disabled={isUploading}
                        >
                            <X className="h-4 w-4"/>
                        </Button>
                        <Button onClick={handleUpload} disabled={isUploading}>
                            {isUploading ? (
                                <>
                                    <Loader2 className="mr-2 h-4 w-4 animate-spin"/>
                                    Processing...
                                </>
                            ) : (
                                <>
                                    <Upload className="mr-2 h-4 w-4"/>
                                    Extract Data
                                </>
                            )}
                        </Button>
                    </div>
                </div>
            ) : (
                <div className="text-center">
                    <Upload className="mx-auto h-10 w-10 text-muted-foreground"/>
                    <p className="mt-3 font-medium text-foreground">
                        Drag and drop a PDF file here
                    </p>
                    <p className="mt-1 text-sm text-muted-foreground">
                        Or click to browse files
                    </p>
                    <Button
                        variant="outline"
                        className="mt-4"
                        onClick={() => fileInputRef.current?.click()}
                    >
                        Select PDF
                    </Button>
                </div>
            )}
        </Card>
    );
};

export default PdfUpload;
