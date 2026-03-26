import {useState, useRef} from 'react';

interface Props {
    onUpload: (file: File) => Promise<void>;
}

const PdfUpload = ({onUpload}: Props) => {
    const [dragging, setDragging] = useState(false);
    const [uploading, setUploading] = useState(false);
    const [fileName, setFileName] = useState<string | null>(null);
    const inputRef = useRef<HTMLInputElement>(null);

    const handleFile = async (file: File) => {
        if (file.type !== 'application/pdf') {
            return;
        }
        setFileName(file.name);
        setUploading(true);
        try {
            await onUpload(file);
        } finally {
            setUploading(false);
            setFileName(null);
        }
    };

    return (
        <div
            onDragOver={(e) => {
                e.preventDefault();
                setDragging(true);
            }}
            onDragLeave={() => setDragging(false)}
            onDrop={(e) => {
                e.preventDefault();
                setDragging(false);
                const file = e.dataTransfer.files[0];
                if (file) handleFile(file);
            }}
            onClick={() => inputRef.current?.click()}
            className={`cursor-pointer rounded-2xl border-2 border-dashed p-12 text-center transition-all ${
                dragging
                    ? 'border-primary bg-primary/5'
                    : 'border-border hover:border-primary/30 hover:bg-muted/30'
            }`}
        >
            <input
                ref={inputRef}
                type="file"
                accept=".pdf"
                className="hidden"
                onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) handleFile(file);
                    e.target.value = '';
                }}
            />

            {uploading ? (
                <div className="flex flex-col items-center gap-3">
                    <div className="h-10 w-10 animate-spin rounded-full border-4 border-muted border-t-primary"/>
                    <p className="text-sm font-medium text-foreground">Extracting data from {fileName}…</p>
                    <p className="text-xs text-muted-foreground">The AI is reading your document</p>
                </div>
            ) : (
                <div className="flex flex-col items-center gap-3">
                    <div className="flex h-14 w-14 items-center justify-center rounded-full bg-primary/10">
                        <svg className="h-7 w-7 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                                  d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
                        </svg>
                    </div>
                    <div>
                        <p className="text-sm font-medium text-foreground">
                            Drop a PDF here or click to browse
                        </p>
                        <p className="mt-1 text-xs text-muted-foreground">
                            The AI will extract structured data matching your table schema
                        </p>
                    </div>
                </div>
            )}
        </div>
    );
};

export default PdfUpload;
