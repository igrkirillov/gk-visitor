package ru.x5.gk.visitor;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResultData {

    @Getter
    private final String[] headers;
    @Getter
    private final List<Object[]> rows = new ArrayList<>();

    private Object[] currentRow;

    public void newRow() {
        if (currentRow != null) {
            rows.add(currentRow);
        }
        currentRow = new Object[headers.length];
    }

    public void addColValue(String header, Object value) {
        int index = 0;
        for (String h : headers) {
            if (h.equals(header)) {
                break;
            }
            ++index;
        }
        currentRow[index] = value;
    }

    public void flush() {
        if (currentRow != null) {
            rows.add(currentRow);
        }
        currentRow = null;
    }

    public String getCurrentRowInStringFormat() {
        if (currentRow != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < headers.length; ++i) {
                sb.append(headers[i] + "=" + currentRow[i] + "; ");
            }
            return sb.toString();
        } else {
            return "nothing";
        }
    }
}
